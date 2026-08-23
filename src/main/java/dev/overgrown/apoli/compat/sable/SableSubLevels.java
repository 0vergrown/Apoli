package dev.overgrown.apoli.compat.sable;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class SableSubLevels {

    private SableSubLevels() {}

    private static volatile Method transformPosition;
    private static volatile Method transformPositionInverse;

    public static boolean available() {
        return Bridge.INSTANCE != null;
    }

    public static @Nullable UUID subLevelAt(Level level, Vec3 pos) {
        Bridge bridge = Bridge.INSTANCE;
        if (bridge == null) return null;
        try {
            Object subLevel = bridge.getContaining.invoke(bridge.helper, level, pos);
            if (subLevel == null) return null;
            return (UUID) bridge.getUniqueId.invoke(subLevel);
        } catch (Throwable t) {
            return null;
        }
    }

    public static @Nullable Vec3 toWorld(Level level, UUID subLevel, Vec3 local) {
        return transform(level, subLevel, local, false);
    }

    public static @Nullable Vec3 toLocal(Level level, UUID subLevel, Vec3 world) {
        return transform(level, subLevel, world, true);
    }

    private static @Nullable Vec3 transform(Level level, UUID subLevel, Vec3 point, boolean inverse) {
        Bridge bridge = Bridge.INSTANCE;
        if (bridge == null) return null;
        try {
            Object container = bridge.getContainer.invoke(null, level);
            if (container == null) return null;
            Object handle = bridge.getSubLevel.invoke(container, subLevel);
            if (handle == null) return null;
            Object pose = bridge.logicalPose.invoke(handle);
            if (pose == null) return null;
            Method method = poseMethod(pose, inverse);
            if (method == null) return null;
            return (Vec3) method.invoke(pose, point);
        } catch (Throwable t) {
            return null;
        }
    }

    private static @Nullable Method poseMethod(Object pose, boolean inverse) {
        Method cached = inverse ? transformPositionInverse : transformPosition;
        if (cached != null) return cached;
        String name = inverse ? "transformPositionInverse" : "transformPosition";
        Method found = findVec3Method(pose.getClass(), name);
        if (found == null) return null;
        if (inverse) transformPositionInverse = found;
        else transformPosition = found;
        return found;
    }

    private static @Nullable Method findVec3Method(Class<?> owner, String name) {
        for (Method method : owner.getMethods()) {
            if (!method.getName().equals(name)) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || params[0] != Vec3.class) continue;
            if (method.getReturnType() != Vec3.class) continue;
            return method;
        }
        return null;
    }

    private static final class Bridge {
        static final Bridge INSTANCE = resolve();

        final Object helper;
        final Method getContaining;
        final Method getUniqueId;
        final Method getContainer;
        final Method getSubLevel;
        final Method logicalPose;

        private Bridge(Object helper, Method getContaining, Method getUniqueId,
                       Method getContainer, Method getSubLevel, Method logicalPose) {
            this.helper = helper;
            this.getContaining = getContaining;
            this.getUniqueId = getUniqueId;
            this.getContainer = getContainer;
            this.getSubLevel = getSubLevel;
            this.logicalPose = logicalPose;
        }

        private static @Nullable Bridge resolve() {
            if (!classPresent("dev/ryanhcode/sable/api/sublevel/SubLevelContainer")) return null;
            try {
                Class<?> sable = Class.forName("dev.ryanhcode.sable.Sable");
                Field helperField = sable.getField("HELPER");
                Object helper = helperField.get(null);
                if (helper == null) return null;

                Method getContaining = findMethod(helper.getClass(), "getContaining", Level.class, Vec3.class);
                Class<?> containerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
                Method getContainer = findMethod(containerClass, "getContainer", Level.class);
                Method getSubLevel = findMethod(containerClass, "getSubLevel", UUID.class);
                if (getContaining == null || getContainer == null || getSubLevel == null) return null;

                Class<?> subLevelClass = getContaining.getReturnType();
                Method getUniqueId = subLevelClass.getMethod("getUniqueId");
                Method logicalPose = subLevelClass.getMethod("logicalPose");
                return new Bridge(helper, getContaining, getUniqueId, getContainer, getSubLevel, logicalPose);
            } catch (Throwable t) {
                return null;
            }
        }

        private static boolean classPresent(String internalName) {
            return SableSubLevels.class.getClassLoader().getResource(internalName + ".class") != null;
        }

        private static @Nullable Method findMethod(Class<?> owner, String name, Class<?>... argumentTypes) {
            for (Method method : owner.getMethods()) {
                if (!method.getName().equals(name)) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length != argumentTypes.length) continue;
                boolean matches = true;
                for (int i = 0; i < params.length; i++) {
                    if (!params[i].isAssignableFrom(argumentTypes[i])) { matches = false; break; }
                }
                if (matches) return method;
            }
            return null;
        }
    }
}
