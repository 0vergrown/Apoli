package dev.overgrown.apoli.client.speech;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class VoskTranscriber {
    private static final float SAMPLE_RATE = 16000.0F;
    private static final int CHUNK_BYTES = 1024;
    private static final long UTTERANCE_GAP_MS = 350L;
    private static final long PARTIAL_INTERVAL_MS = 60L;

    public enum Source { MICROPHONE, VOICE_CHAT }

    public interface Listener {
        void text(String text, boolean complete);

        void utteranceEnded();
    }

    private final Path modelDir;
    private final String deviceHint;
    private final Source source;
    private final float sampleRate;
    private final BlockingQueue<short[]> incoming = new ArrayBlockingQueue<>(128);
    private final Listener listener;
    private String lastPartial = "";
    private long nextPartial;

    private Thread thread;
    private volatile boolean running;
    private volatile boolean listening;
    private volatile boolean failed;
    private volatile boolean silentCapture;
    private volatile String failure;
    private String lineName = "default";

    public VoskTranscriber(Path modelDir, String deviceHint, Source source, Listener listener) {
        this.modelDir = modelDir;
        this.deviceHint = deviceHint == null ? "" : deviceHint;
        this.source = source;
        this.sampleRate = source == Source.VOICE_CHAT
            ? dev.overgrown.apoli.compat.voicechat.SpeechAudioBus.SAMPLE_RATE
            : SAMPLE_RATE;
        this.listener = listener;
    }

    public void offer(short[] pcm) {
        incoming.offer(pcm);
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::run, "Apoli-Speech");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        listening = false;
        Thread t = thread;
        if (t != null) t.interrupt();
        thread = null;
    }

    public void setListening(boolean value) {
        listening = value;
    }

    public boolean hasFailed() {
        return failed;
    }

    public String failure() {
        String message = failure;
        return message == null ? "unknown error" : message;
    }

    public boolean consumeSilentCapture() {
        if (!silentCapture) return false;
        silentCapture = false;
        return true;
    }

    private void run() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        Model model = null;
        Recognizer recognizer = null;
        TargetDataLine line = null;
        try {
            Path resolved = resolveModel(modelDir);
            model = new Model(resolved.toString());
            recognizer = new Recognizer(model, sampleRate);
            if (source == Source.VOICE_CHAT) {
                Apoli.LOGGER.info("[Apoli] Speech-to-action ready (Vosk model {}, Simple Voice Chat microphone).",
                    resolved.getFileName());
                runVoiceChat(recognizer);
                return;
            }
            logDevices(format);
            Apoli.LOGGER.info("[Apoli] Speech-to-action ready (Vosk model {}).", resolved.getFileName());

            byte[] buffer = new byte[CHUNK_BYTES];
            long captured = 0L;

            while (running) {
                if (!listening) {
                    if (line != null) {
                        captured += drain(line, recognizer, buffer);
                        line.stop();
                        line.close();
                        line = null;
                        Apoli.LOGGER.info("[Apoli] Speech capture ended ({} bytes).", captured);
                        if (captured == 0L) {
                            silentCapture = true;
                            Apoli.LOGGER.warn("[Apoli] Captured no audio — another application (Simple Voice Chat?) "
                                + "may hold the microphone. Set \"speechInputDevice\" in apoli-client.json to pick "
                                + "a different recording device.");
                        }
                        finish(recognizer);
                        captured = 0L;
                    }
                    Thread.sleep(20L);
                    continue;
                }
                if (line == null) {
                    line = openLine(format);
                    recognizer.reset();
                    captured = 0L;
                    line.start();
                    resetPartial();
                    Apoli.LOGGER.info("[Apoli] Speech capture started ({}).", lineName);
                }
                int read = line.read(buffer, 0, buffer.length);
                if (read <= 0) continue;
                captured += read;
                if (recognizer.acceptWaveForm(buffer, read)) {
                    emit(recognizer.getResult(), true);
                    listener.utteranceEnded();
                    resetPartial();
                } else {
                    pollPartial(recognizer, System.currentTimeMillis());
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            failure = t.getClass().getSimpleName() + ": " + t.getMessage();
            failed = true;
            Apoli.LOGGER.error("[Apoli] Speech-to-action stopped", t);
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
            if (recognizer != null) recognizer.close();
            if (model != null) model.close();
        }
    }

    private void runVoiceChat(Recognizer recognizer) throws InterruptedException {
        long lastAudio = 0L;
        long frames = 0L;
        boolean speaking = false;
        while (running) {
            short[] chunk = incoming.poll(100L, TimeUnit.MILLISECONDS);
            long now = System.currentTimeMillis();
            if (chunk == null) {
                if (speaking && now - lastAudio >= UTTERANCE_GAP_MS) {
                    speaking = false;
                    Apoli.LOGGER.info("[Apoli] Speech capture ended ({} samples via voice chat).", frames);
                    frames = 0L;
                    finish(recognizer);
                }
                continue;
            }
            if (!speaking) {
                speaking = true;
                frames = 0L;
                recognizer.reset();
                resetPartial();
                Apoli.LOGGER.info("[Apoli] Speech capture started (Simple Voice Chat).");
            }
            lastAudio = now;
            frames += chunk.length;
            if (recognizer.acceptWaveForm(chunk, chunk.length)) {
                emit(recognizer.getResult(), true);
                listener.utteranceEnded();
                resetPartial();
            } else {
                pollPartial(recognizer, now);
            }
        }
    }

    private TargetDataLine openLine(AudioFormat format) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!deviceHint.isBlank()) {
            String want = deviceHint.toLowerCase(Locale.ROOT);
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                if (!mixerInfo.getName().toLowerCase(Locale.ROOT).contains(want)) continue;
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (!mixer.isLineSupported(info)) continue;
                TargetDataLine picked = (TargetDataLine) mixer.getLine(info);
                picked.open(format);
                lineName = mixerInfo.getName();
                return picked;
            }
            Apoli.LOGGER.warn("[Apoli] No recording device matched \"{}\" — falling back to the system default.",
                deviceHint);
        }
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("no recording device supports 16 kHz mono 16-bit capture");
        }
        TargetDataLine picked = (TargetDataLine) AudioSystem.getLine(info);
        picked.open(format);
        lineName = "system default";
        return picked;
    }

    private static void logDevices(AudioFormat format) {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        StringBuilder found = new StringBuilder();
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            if (!AudioSystem.getMixer(mixerInfo).isLineSupported(info)) continue;
            if (found.length() > 0) found.append(" | ");
            found.append(mixerInfo.getName());
        }
        Apoli.LOGGER.info("[Apoli] Recording devices available for speech: {}",
            found.length() == 0 ? "(none)" : found);
    }

    private long drain(TargetDataLine line, Recognizer recognizer, byte[] buffer) {
        long total = 0L;
        for (int guard = 0; guard < 128; guard++) {
            int chunk = frames(Math.min(line.available(), buffer.length));
            if (chunk <= 0) return total;
            int read = line.read(buffer, 0, chunk);
            if (read <= 0) return total;
            total += read;
            recognizer.acceptWaveForm(buffer, read);
        }
        return total;
    }

    private static int frames(int bytes) {
        return bytes < 2 ? 0 : bytes - (bytes % 2);
    }

    private static Path resolveModel(Path dir) {
        if (Files.isDirectory(dir.resolve("conf"))) return dir;
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> subdirs = stream.filter(Files::isDirectory).toList();
            if (subdirs.size() == 1 && Files.isDirectory(subdirs.get(0).resolve("conf"))) return subdirs.get(0);
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Could not inspect Vosk model dir {}: {}", dir, e.getMessage());
        }
        return dir;
    }

    private void pollPartial(Recognizer recognizer, long now) {
        if (now < nextPartial) return;
        nextPartial = now + PARTIAL_INTERVAL_MS;
        String text = extract(recognizer.getPartialResult(), "partial");
        if (text == null || text.isBlank() || text.equals(lastPartial)) return;
        lastPartial = text;
        listener.text(text, false);
    }

    private void resetPartial() {
        lastPartial = "";
        nextPartial = 0L;
    }

    private void finish(Recognizer recognizer) {
        emit(recognizer.getFinalResult(), true);
        listener.utteranceEnded();
        resetPartial();
    }

    private void emit(String json, boolean complete) {
        String text = extract(json, "text");
        if (text != null && !text.isBlank()) {
            Apoli.LOGGER.info("[Apoli] Speech heard: \"{}\"", text);
            listener.text(text, complete);
        }
    }

    private static String extract(String json, String field) {
        try {
            Dynamic<?> obj = new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseString(json));
            return obj.get(field).asString().result().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
