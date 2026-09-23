package dev.overgrown.apoli.compat.entitymodelfeatures;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.ModCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.reflect.Method;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class EntityModelFeaturesCompat {

    private static boolean hooked;
    private static int held;

    private EntityModelFeaturesCompat() {}

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
    }

    public static void holdPose() {
        if (hooked) {
            held++;
        }
    }

    public static void releasePose() {
        if (hooked && held > 0) {
            held--;
        }
    }
}
