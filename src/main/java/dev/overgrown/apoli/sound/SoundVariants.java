package dev.overgrown.apoli.sound;

import dev.overgrown.apoli.Apoli;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class SoundVariants {

    private static final String PREFIX = "sound_variant/";

    private SoundVariants() {}

    public static ResourceLocation encode(ResourceLocation sound, int index) {
        return Apoli.id(PREFIX + index + "/" + sound.getNamespace() + "/" + sound.getPath());
    }

    public static boolean isVariant(ResourceLocation location) {
        return Apoli.MOD_ID.equals(location.getNamespace()) && location.getPath().startsWith(PREFIX);
    }

    public static int index(ResourceLocation variant) {
        String path = variant.getPath();
        int end = path.indexOf('/', PREFIX.length());
        if (end <= PREFIX.length()) return -1;
        try {
            return Integer.parseInt(path, PREFIX.length(), end, 10);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Nullable
    public static ResourceLocation base(ResourceLocation variant) {
        String path = variant.getPath();
        int indexEnd = path.indexOf('/', PREFIX.length());
        if (indexEnd < 0) return null;
        int namespaceEnd = path.indexOf('/', indexEnd + 1);
        if (namespaceEnd < 0) return null;
        return ResourceLocation.tryBuild(path.substring(indexEnd + 1, namespaceEnd), path.substring(namespaceEnd + 1));
    }

    public static ResourceLocation unwrap(ResourceLocation location) {
        if (!isVariant(location)) return location;
        ResourceLocation base = base(location);
        return base != null ? base : location;
    }
}
