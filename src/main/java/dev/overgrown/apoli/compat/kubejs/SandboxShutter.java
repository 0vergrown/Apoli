package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.rhino.ClassShutter;

public final class SandboxShutter implements ClassShutter {

    private static final String[] ALLOWED_PREFIXES = {
        "net.minecraft.",
        "dev.overgrown.apoli.",
        "com.mojang.math.",
        "org.joml.",
        "java.lang.String",
        "java.lang.Math",
        "java.lang.Number",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Float",
        "java.lang.Boolean",
        "java.util.List",
        "java.util.Map",
        "java.util.Set",
        "java.util.Optional",
        "java.util.UUID"
    };

    private static final String[] DENIED_PREFIXES = {
        "net.minecraft.server.MinecraftServer",
        "net.minecraft.server.packs.",
        "net.minecraft.world.level.storage.LevelStorageSource"
    };

    @Override
    public boolean visibleToScripts(String fullClassName, int type) {
        for (String denied : DENIED_PREFIXES) {
            if (fullClassName.startsWith(denied)) return false;
        }
        for (String allowed : ALLOWED_PREFIXES) {
            if (fullClassName.startsWith(allowed)) return true;
        }
        return false;
    }
}
