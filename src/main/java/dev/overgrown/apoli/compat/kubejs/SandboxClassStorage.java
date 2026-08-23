package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.rhino.CachedClassStorage;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public final class SandboxClassStorage extends CachedClassStorage {

    private static final String[] DENIED_PREFIXES = {
        "java.io.",
        "java.nio.file.",
        "java.lang.Class",
        "java.lang.ClassLoader",
        "java.lang.Module",
        "java.lang.Package",
        "java.lang.Process",
        "java.lang.Runtime",
        "java.lang.SecurityManager",
        "java.lang.System",
        "java.lang.Thread",
        "java.lang.invoke.",
        "java.lang.reflect.",
        "java.net.",
        "java.util.concurrent.",
        "java.util.jar.",
        "java.util.zip.",
        "javax.script.",
        "jdk.",
        "sun.",
        "com.sun.",
        "org.apache.logging.",
        "net.minecraft.server.MinecraftServer",
        "net.minecraft.server.packs.",
        "net.minecraft.world.level.storage.LevelStorageSource",
        "net.neoforged.fml.",
        "cpw.mods."
    };

    public SandboxClassStorage() {
        super(false);
    }

    @Override
    public boolean include(Class<?> type, Member member) {
        if (!super.include(type, member)) return false;
        if (denied(member.getDeclaringClass())) return false;
        if (member instanceof Method method) {
            if (denied(method.getReturnType())) return false;
            for (Class<?> parameter : method.getParameterTypes()) {
                if (denied(parameter)) return false;
            }
            return true;
        }
        if (member instanceof Field field) {
            return !denied(field.getType());
        }
        return true;
    }

    public static boolean denied(Class<?> type) {
        if (type == null || type.isPrimitive()) return false;
        Class<?> resolved = type;
        while (resolved.isArray()) resolved = resolved.getComponentType();
        if (resolved.isPrimitive()) return false;
        String name = resolved.getName();
        for (String prefix : DENIED_PREFIXES) {
            if (name.startsWith(prefix)) return true;
        }
        return false;
    }
}
