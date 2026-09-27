package dev.overgrown.apoli.util;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Optional;

public final class InteractionRange {
    private InteractionRange() {}

    public static double block(Entity entity) {
        return of(entity, Attributes.BLOCK_INTERACTION_RANGE);
    }

    public static double entity(Entity entity) {
        return of(entity, Attributes.ENTITY_INTERACTION_RANGE);
    }

    public static float block(Entity source, Optional<Float> own, Optional<Float> shared) {
        if (own.isPresent()) return own.get();
        return shared.isPresent() ? shared.get() : (float) block(source);
    }

    public static float entity(Entity source, Optional<Float> own, Optional<Float> shared) {
        if (own.isPresent()) return own.get();
        return shared.isPresent() ? shared.get() : (float) entity(source);
    }

    private static double of(Entity entity, Holder<Attribute> attribute) {
        if (entity instanceof LivingEntity living) {
            AttributeInstance instance = living.getAttribute(attribute);
            if (instance != null) return instance.getValue();
        }
        return attribute.value().getDefaultValue();
    }
}
