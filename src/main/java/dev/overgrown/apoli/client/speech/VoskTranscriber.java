package dev.overgrown.apoli.client.speech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.overgrown.apoli.Apoli;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public final class VoskTranscriber {
    private static final float SAMPLE_RATE = 16000.0F;

    private final Path modelDir;
    private final String language;
    private Thread thread;
    private volatile boolean running;

    public VoskTranscriber(Path modelDir, String language) {
        this.modelDir = modelDir;
        this.language = language;
    }

    public void start(BiConsumer<String, String> onTranscript) {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(() -> run(onTranscript), "Apoli-Vosk");
        thread.setDaemon(true);
        thread.start();
    }

    private void run(BiConsumer<String, String> onTranscript) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        Model model = null;
        Recognizer recognizer = null;
        TargetDataLine line = null;
        try {
            Path resolved = resolveModel(modelDir);
            model = new Model(resolved.toString());
            recognizer = new Recognizer(model, SAMPLE_RATE);
            line = AudioSystem.getTargetDataLine(format);
            line.open(format);
            line.start();
            Apoli.LOGGER.info("[Apoli] Vosk offline speech started ({}).", resolved);
            byte[] buffer = new byte[4096];
            while (running) {
                int read = line.read(buffer, 0, buffer.length);
                if (read > 0 && recognizer.acceptWaveForm(buffer, read)) {
                    String text = extract(recognizer.getResult());
                    if (text != null && !text.isBlank()) {
                        onTranscript.accept(text, language);
                    }
                }
            }
        } catch (Throwable t) {
            Apoli.LOGGER.error("[Apoli] Vosk transcriber failed: {}", t.getMessage());
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
            if (recognizer != null) {
                recognizer.close();
            }
            if (model != null) {
                model.close();
            }
        }
    }

    public void stop() {
        running = false;
    }

    private static Path resolveModel(Path dir) {
        if (Files.isDirectory(dir.resolve("conf"))) {
            return dir;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> subdirs = stream.filter(Files::isDirectory).toList();
            if (subdirs.size() == 1 && Files.isDirectory(subdirs.get(0).resolve("conf"))) {
                return subdirs.get(0);
            }
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Could not inspect Vosk model dir {}: {}", dir, e.getMessage());
        }
        return dir;
    }

    private static String extract(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.has("text") ? obj.get("text").getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
