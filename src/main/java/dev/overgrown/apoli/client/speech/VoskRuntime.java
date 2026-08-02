package dev.overgrown.apoli.client.speech;

import dev.overgrown.apoli.Apoli;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class VoskRuntime {

    private static final String[] LIBS = {"jna.jar", "vosk.jar"};

    private static boolean attempted;
    private static @Nullable Bridge bridge;
    private static @Nullable String failure;

    private VoskRuntime() {}

    public static synchronized boolean available() {
        return bridge() != null;
    }

    public static synchronized @Nullable String failure() {
        bridge();
        return failure;
    }

    public static Session open(String modelPath, float sampleRate) throws Exception {
        Bridge b = bridge();
        if (b == null) throw new IllegalStateException("Vosk runtime unavailable: " + failure);
        return new Session(b, modelPath, sampleRate);
    }

    private static synchronized @Nullable Bridge bridge() {
        if (attempted) return bridge;
        attempted = true;
        try {
            bridge = new Bridge(loader());
        } catch (Throwable t) {
            failure = t.getClass().getSimpleName() + ": " + t.getMessage();
            Apoli.LOGGER.error("[Apoli] Could not start the Vosk runtime", t);
        }
        return bridge;
    }

    private static ClassLoader loader() throws Exception {
        Path dir = FMLPaths.GAMEDIR.get().resolve("apoli").resolve("runtime");
        Files.createDirectories(dir);
        URL[] urls = new URL[LIBS.length];
        for (int i = 0; i < LIBS.length; i++) {
            urls[i] = extract(dir, LIBS[i]).toUri().toURL();
        }
        return new IsolatedLoader(urls, VoskRuntime.class.getClassLoader());
    }

    private static Path extract(Path dir, String name) throws Exception {
        Path target = dir.resolve(name);
        URL source = VoskRuntime.class.getResource("/apoli/runtime/" + name);
        if (source == null) {
            if (Files.isRegularFile(target)) return target;
            throw new IllegalStateException("Missing bundled library apoli/runtime/" + name);
        }
        long expected = source.openConnection().getContentLengthLong();
        if (expected >= 0 && Files.isRegularFile(target) && Files.size(target) == expected) {
            return target;
        }
        Path tmp = dir.resolve(name + ".tmp");
        try (InputStream in = source.openStream(); OutputStream out = Files.newOutputStream(tmp)) {
            in.transferTo(out);
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        Apoli.LOGGER.info("[Apoli] Unpacked speech library {} ({} bytes).", name, Files.size(target));
        return target;
    }

    private static final class IsolatedLoader extends URLClassLoader {
        static {
            registerAsParallelCapable();
        }

        IsolatedLoader(URL[] urls, ClassLoader parent) {
            super("apoli-vosk", urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("com.sun.jna.") || name.startsWith("org.vosk.")) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) loaded = findClass(name);
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
            return super.loadClass(name, resolve);
        }
    }

    private record Bridge(
        Constructor<?> newModel,
        Constructor<?> newRecognizer,
        Method reset,
        Method acceptWaveForm,
        Method acceptWaveFormShort,
        Method getResult,
        Method getPartialResult,
        Method getFinalResult,
        Method closeModel,
        Method closeRecognizer
    ) {
        Bridge(ClassLoader cl) throws Exception {
            this(
                Class.forName("org.vosk.Model", true, cl).getConstructor(String.class),
                Class.forName("org.vosk.Recognizer", true, cl)
                    .getConstructor(Class.forName("org.vosk.Model", true, cl), float.class),
                Class.forName("org.vosk.Recognizer", true, cl).getMethod("reset"),
                Class.forName("org.vosk.Recognizer", true, cl).getMethod("acceptWaveForm", byte[].class, int.class),
                Class.forName("org.vosk.Recognizer", true, cl).getMethod("acceptWaveForm", short[].class, int.class),
                Class.forName("org.vosk.Recognizer", true, cl).getMethod("getResult"),
                Class.forName("org.vosk.Recognizer", true, cl).getMethod("getPartialResult"),
                Class.forName("org.vosk.Recognizer", true, cl).getMethod("getFinalResult"),
                Class.forName("org.vosk.Model", true, cl).getMethod("close"),
                Class.forName("org.vosk.Recognizer", true, cl).getMethod("close")
            );
        }
    }

    public static final class Session implements AutoCloseable {
        private final Bridge bridge;
        private final Object model;
        private final Object recognizer;

        Session(Bridge bridge, String modelPath, float sampleRate) throws Exception {
            this.bridge = bridge;
            this.model = bridge.newModel().newInstance(modelPath);
            this.recognizer = bridge.newRecognizer().newInstance(model, sampleRate);
        }

        public void reset() throws Exception {
            bridge.reset().invoke(recognizer);
        }

        public boolean acceptWaveForm(byte[] data, int length) throws Exception {
            return (Boolean) bridge.acceptWaveForm().invoke(recognizer, data, length);
        }

        public boolean acceptWaveForm(short[] data, int length) throws Exception {
            return (Boolean) bridge.acceptWaveFormShort().invoke(recognizer, data, length);
        }

        public String result() throws Exception {
            return (String) bridge.getResult().invoke(recognizer);
        }

        public String partialResult() throws Exception {
            return (String) bridge.getPartialResult().invoke(recognizer);
        }

        public String finalResult() throws Exception {
            return (String) bridge.getFinalResult().invoke(recognizer);
        }

        @Override
        public void close() {
            try {
                bridge.closeRecognizer().invoke(recognizer);
            } catch (Exception ignored) {
            }
            try {
                bridge.closeModel().invoke(model);
            } catch (Exception ignored) {
            }
        }
    }
}
