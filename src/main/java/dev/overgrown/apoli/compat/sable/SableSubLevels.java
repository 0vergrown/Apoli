package dev.overgrown.apoli.compat.sable;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

public final class SableSubLevels {

    private SableSubLevels() {}

    private static volatile Method transformPosition;
    private static volatile Method transformPositionInverse;
    private static volatile Method transformNormalInverse;
    private static volatile Method massTracker;
    private static volatile Method massValue;

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

    public static boolean exists(Level level, UUID subLevel) {
        return resolve(level, subLevel) != null;
    }

    public static @Nullable Vec3 toWorld(Level level, UUID subLevel, Vec3 local) {
        Bridge bridge = Bridge.INSTANCE;
        Object handle = resolve(level, subLevel);
        if (bridge == null || handle == null) return null;
        if (bridge.projectOutOfSubLevel != null) {
            try {
                Vec3 projected = (Vec3) bridge.projectOutOfSubLevel.invoke(bridge.helper, level, local);
                if (projected != null) return projected;
            } catch (Throwable ignored) {
            }
        }
        return transform(handle, local, false);
    }

    public static @Nullable Vec3 toLocal(Level level, UUID subLevel, Vec3 world) {
        Object handle = resolve(level, subLevel);
        return handle == null ? null : transform(handle, world, true);
    }

    public static boolean pull(Level level, UUID subLevel, Vec3 localPoint, Vec3 worldDirection,
                               double speed, double forceScale) {
        Bridge bridge = Bridge.INSTANCE;
        if (bridge == null || bridge.rigidBodyOf == null || bridge.applyImpulseAtPoint == null) return false;
        if (worldDirection.lengthSqr() < 1.0e-8) return false;
        Object handle = resolve(level, subLevel);
        if (handle == null || !bridge.rigidBodyOf.getParameterTypes()[0].isInstance(handle)) return false;
        try {
            Object body = bridge.rigidBodyOf.invoke(null, handle);
            if (body == null) return false;
            Object pose = bridge.logicalPose.invoke(handle);
            if (pose == null) return false;
            Method normal = normalMethod(pose);
            if (normal == null) return false;
            double magnitude = speed * forceScale * mass(handle);
            if (magnitude == 0) return false;
            Vec3 impulse = (Vec3) normal.invoke(pose, worldDirection.normalize().scale(magnitude));
            if (impulse == null) return false;
            bridge.applyImpulseAtPoint.invoke(body, localPoint, impulse);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static double mass(Object subLevel) {
        try {
            Method tracker = massTracker;
            if (tracker == null) {
                tracker = subLevel.getClass().getMethod("getMassTracker");
                massTracker = tracker;
            }
            Object data = tracker.invoke(subLevel);
            if (data == null) return 1.0;
            Method value = massValue;
            if (value == null) {
                value = data.getClass().getMethod("getMass");
                massValue = value;
            }
            double amount = ((Number) value.invoke(data)).doubleValue();
            return amount > 0 ? amount : 1.0;
        } catch (Throwable t) {
            return 1.0;
        }
    }

    private static @Nullable Object resolve(Level level, UUID subLevel) {
        Bridge bridge = Bridge.INSTANCE;
        if (bridge == null) return null;
        try {
            Object container = bridge.getContainer.invoke(null, level);
            if (container == null) return null;
            return bridge.getSubLevel.invoke(container, subLevel);
        } catch (Throwable t) {
            return null;
        }
    }

    private static @Nullable Vec3 transform(Object subLevel, Vec3 point, boolean inverse) {
        Bridge bridge = Bridge.INSTANCE;
        if (bridge == null) return null;
        try {
            Object pose = bridge.logicalPose.invoke(subLevel);
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

    private static @Nullable Method normalMethod(Object pose) {
        Method cached = transformNormalInverse;
        if (cached != null) return cached;
        Method found = findVec3Method(pose.getClass(), "transformNormalInverse");
        if (found != null) transformNormalInverse = found;
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
        final @Nullable Method projectOutOfSubLevel;
        final @Nullable Method rigidBodyOf;
        final @Nullable Method applyImpulseAtPoint;

        private Bridge(Object helper, Method getContaining, Method getUniqueId,
                       Method getContainer, Method getSubLevel, Method logicalPose,
                       @Nullable Method projectOutOfSubLevel, @Nullable Method rigidBodyOf,
                       @Nullable Method applyImpulseAtPoint) {
            this.helper = helper;
            this.getContaining = getContaining;
            this.getUniqueId = getUniqueId;
            this.getContainer = getContainer;
            this.getSubLevel = getSubLevel;
            this.logicalPose = logicalPose;
            this.projectOutOfSubLevel = projectOutOfSubLevel;
            this.rigidBodyOf = rigidBodyOf;
            this.applyImpulseAtPoint = applyImpulseAtPoint;
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

                Method projectOutOfSubLevel = null;
                for (Method method : helper.getClass().getMethods()) {
                    if (!method.getName().equals("projectOutOfSubLevel")) continue;
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length != 2 || params[0] != Level.class || params[1] != Vec3.class) continue;
                    if (method.getReturnType() != Vec3.class) continue;
                    projectOutOfSubLevel = method;
                    break;
                }

                Method rigidBodyOf = null;
                Method applyImpulseAtPoint = null;
                try {
                    Class<?> handleClass = Class.forName("dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle");
                    for (Method method : handleClass.getMethods()) {
                        if (Modifier.isStatic(method.getModifiers()) && method.getName().equals("of")
                            && method.getParameterCount() == 1) {
                            rigidBodyOf = method;
                        } else if (!Modifier.isStatic(method.getModifiers())
                            && method.getName().equals("applyImpulseAtPoint")
                            && method.getParameterCount() == 2
                            && method.getParameterTypes()[0] == Vec3.class
                            && method.getParameterTypes()[1] == Vec3.class) {
                            applyImpulseAtPoint = method;
                        }
                    }
                } catch (Throwable ignored) {
                }

                return new Bridge(helper, getContaining, getUniqueId, getContainer, getSubLevel, logicalPose,
                    projectOutOfSubLevel, rigidBodyOf, applyImpulseAtPoint);
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
