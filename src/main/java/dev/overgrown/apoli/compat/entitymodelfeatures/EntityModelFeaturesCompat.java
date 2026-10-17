package dev.overgrown.apoli.compat.entitymodelfeatures;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.mixin.custom_model.ModelPartChildrenAccessor;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class EntityModelFeaturesCompat {

    private static final ModelPart[] NO_PARTS = new ModelPart[0];
    private static final Map<ModelPart, CustomChildren> CUSTOM_CHILDREN = new WeakHashMap<>();

    private static boolean hooked;
    private static int held;
    private static Set<UUID> pausedEntities;
    private static MethodHandle currentEntity;
    private static Class<?> customPartClass;
    private static UUID pausedByApoli;

    private EntityModelFeaturesCompat() {}

    private record CustomChildren(Map<String, ModelPart> source, int size, ModelPart[] parts) {}

    public static void init() {
        if (hooked || !ModCompat.ENTITY_MODEL_FEATURES) {
            return;
        }
        try {
            Method register = Class.forName("traben.entity_model_features.EMFAnimationApi")
                .getMethod("registerPauseCondition", Function.class);
            register.invoke(null, (Function<Object, Boolean>) entity -> held > 0);
            hooked = true;
        } catch (Throwable failure) {
            Apoli.LOGGER.warn("[Apoli] Entity Model Features is installed but its animation-pause API could not be reached ({}). "
                + "Texture overlays and custom geometry may drift out of alignment with CEM animations such as Fresh Animations.",
                failure.toString());
        }
        pausedEntities = resolvePausedEntities();
        currentEntity = resolveCurrentEntity();
        customPartClass = resolveCustomPartClass();
    }

    @SuppressWarnings("unchecked")
    private static Set<UUID> resolvePausedEntities() {
        try {
            Object paused = Class.forName("traben.entity_model_features.utils.EMFAnimationPauseHandler")
                .getField("entitiesPaused").get(null);
            return paused instanceof Set<?> set ? (Set<UUID>) set : null;
        } catch (Throwable failure) {
            return null;
        }
    }

    private static MethodHandle resolveCurrentEntity() {
        try {
            Method current = Class.forName("traben.entity_model_features.EMFAnimationApi").getMethod("getCurrentEntity");
            return MethodHandles.publicLookup().unreflect(current).asType(MethodType.methodType(Object.class));
        } catch (Throwable failure) {
            return null;
        }
    }

    private static Class<?> resolveCustomPartClass() {
        try {
            return Class.forName("traben.entity_model_features.models.parts.EMFModelPartCustom");
        } catch (Throwable failure) {
            return null;
        }
    }

    public static void holdPose() {
        if (!hooked) {
            return;
        }
        if (held++ == 0) {
            pauseCurrentEntity();
        }
    }

    public static void releasePose() {
        if (!hooked || held == 0) {
            return;
        }
        if (--held == 0 && pausedByApoli != null) {
            pausedEntities.remove(pausedByApoli);
            pausedByApoli = null;
        }
    }

    private static void pauseCurrentEntity() {
        if (pausedEntities == null || currentEntity == null) {
            return;
        }
        try {
            Object current = (Object) currentEntity.invokeExact();
            if (current instanceof Entity entity) {
                UUID id = entity.getUUID();
                if (pausedEntities.add(id)) {
                    pausedByApoli = id;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean hasCustomParts() {
        return customPartClass != null;
    }

    public static ModelPart[] customChildren(ModelPart part) {
        if (customPartClass == null) {
            return NO_PARTS;
        }
        Map<String, ModelPart> children = ((ModelPartChildrenAccessor) (Object) part).apoli$children();
        CustomChildren cached = CUSTOM_CHILDREN.get(part);
        if (cached != null && cached.source() == children && cached.size() == children.size()) {
            return cached.parts();
        }
        int count = 0;
        for (ModelPart child : children.values()) {
            if (customPartClass.isInstance(child)) count++;
        }
        ModelPart[] parts = count == 0 ? NO_PARTS : new ModelPart[count];
        int i = 0;
        for (ModelPart child : children.values()) {
            if (customPartClass.isInstance(child)) parts[i++] = child;
        }
        CUSTOM_CHILDREN.put(part, new CustomChildren(children, children.size(), parts));
        return parts;
    }
}
