package dev.overgrown.apoli.loader;

import org.jetbrains.annotations.Nullable;

public enum JsonFormat {
    JSON(".json"),
    JSONC(".jsonc"),
    JSON5(".json5");

    private final String extension;

    JsonFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public boolean allowsComments() {
        return this != JSON;
    }

    public static @Nullable JsonFormat of(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0) return null;
        int length = path.length() - dot;
        if (length == 5 && path.startsWith(".json", dot)) return JSON;
        if (length == 6 && path.startsWith(".jsonc", dot)) return JSONC;
        if (length == 6 && path.startsWith(".json5", dot)) return JSON5;
        return null;
    }
}
