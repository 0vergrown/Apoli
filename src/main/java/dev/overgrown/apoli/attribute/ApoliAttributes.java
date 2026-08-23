package dev.overgrown.apoli.attribute;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.HashMap;
import java.util.Map;

public final class ApoliAttributes {
    private ApoliAttributes() {}

    public static final double DEFAULT_BLOCK_INTERACTION_RANGE = 4.5;
    public static final double DEFAULT_ENTITY_INTERACTION_RANGE = 3.0;

    public static final ResourceLocation BLOCK_INTERACTION_RANGE_ID = Apoli.id("player.block_interaction_range");
    public static final ResourceLocation ENTITY_INTERACTION_RANGE_ID = Apoli.id("player.entity_interaction_range");

    public static final Attribute BLOCK_INTERACTION_RANGE = new RangedAttribute(
        "attribute.name.player.block_interaction_range", DEFAULT_BLOCK_INTERACTION_RANGE, 0.0, 64.0).setSyncable(true);

    public static final Attribute ENTITY_INTERACTION_RANGE = new RangedAttribute(
        "attribute.name.player.entity_interaction_range", DEFAULT_ENTITY_INTERACTION_RANGE, 0.0, 64.0).setSyncable(true);

    private static final Map<ResourceLocation, ResourceLocation> ALIASES = new HashMap<>();

    static {
        alias("minecraft", "player.block_interaction_range", BLOCK_INTERACTION_RANGE_ID);
        alias("minecraft", "player.entity_interaction_range", ENTITY_INTERACTION_RANGE_ID);
        alias("minecraft", "block_interaction_range", BLOCK_INTERACTION_RANGE_ID);
        alias("minecraft", "entity_interaction_range", ENTITY_INTERACTION_RANGE_ID);
        alias("origins", "player.block_interaction_range", BLOCK_INTERACTION_RANGE_ID);
        alias("origins", "player.entity_interaction_range", ENTITY_INTERACTION_RANGE_ID);
        alias("reach-entity-attributes", "reach", BLOCK_INTERACTION_RANGE_ID);
        alias("reach-entity-attributes", "attack_range", ENTITY_INTERACTION_RANGE_ID);
    }

    private static void alias(String namespace, String path, ResourceLocation target) {
        ALIASES.put(new ResourceLocation(namespace, path), target);
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ATTRIBUTE, BLOCK_INTERACTION_RANGE_ID, BLOCK_INTERACTION_RANGE);
        Registry.register(BuiltInRegistries.ATTRIBUTE, ENTITY_INTERACTION_RANGE_ID, ENTITY_INTERACTION_RANGE);
    }

    public static ResourceLocation resolve(ResourceLocation id) {
        ResourceLocation aliased = ALIASES.get(id);
        return aliased == null ? id : aliased;
    }

    public static Attribute get(ResourceLocation id) {
        return BuiltInRegistries.ATTRIBUTE.get(resolve(id));
    }

    public static double blockInteractionRange(LivingEntity entity) {
        return valueOf(entity, BLOCK_INTERACTION_RANGE, DEFAULT_BLOCK_INTERACTION_RANGE);
    }

    public static double entityInteractionRange(LivingEntity entity) {
        return valueOf(entity, ENTITY_INTERACTION_RANGE, DEFAULT_ENTITY_INTERACTION_RANGE);
    }

    private static double valueOf(LivingEntity entity, Attribute attribute, double fallback) {
        if (entity == null) return fallback;
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }
}
