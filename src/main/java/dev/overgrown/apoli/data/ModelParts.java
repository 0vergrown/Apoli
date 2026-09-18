package dev.overgrown.apoli.data;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class ModelParts {
    private ModelParts() {}

    public static final String HEAD = "head";
    public static final String HAT = "hat";
    public static final String BODY = "body";
    public static final String RIGHT_ARM = "rightarm";
    public static final String LEFT_ARM = "leftarm";
    public static final String RIGHT_LEG = "rightleg";
    public static final String LEFT_LEG = "leftleg";

    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "").replace("-", "");
    }

    @Nullable
    public static String slot(String normalized) {
        BodyPart part = BodyParts.lookup(normalized);
        return part == null ? null : part.bindKey();
    }
}
