package dev.overgrown.apoli.util;

import dev.overgrown.apoli.attribute.ApoliAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class InteractionRange {
    private static final double CREATIVE_BLOCK_BONUS = 0.5;
    private static final double CREATIVE_ENTITY_BONUS = 3.0;

    private InteractionRange() {}

    public static double block(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return ApoliAttributes.DEFAULT_BLOCK_INTERACTION_RANGE;
        return ApoliAttributes.blockInteractionRange(living) + (creative(living) ? CREATIVE_BLOCK_BONUS : 0.0);
    }

    public static double entity(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return ApoliAttributes.DEFAULT_ENTITY_INTERACTION_RANGE;
        return ApoliAttributes.entityInteractionRange(living) + (creative(living) ? CREATIVE_ENTITY_BONUS : 0.0);
    }

    public static float block(Entity source, Optional<Float> own, Optional<Float> shared) {
        if (own.isPresent()) return own.get();
        return shared.isPresent() ? shared.get() : (float) block(source);
    }

    public static float entity(Entity source, Optional<Float> own, Optional<Float> shared) {
        if (own.isPresent()) return own.get();
        return shared.isPresent() ? shared.get() : (float) entity(source);
    }

    private static boolean creative(LivingEntity entity) {
        return entity instanceof Player player && player.getAbilities().instabuild;
    }
}
