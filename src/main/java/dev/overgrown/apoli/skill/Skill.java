package dev.overgrown.apoli.skill;

import dev.overgrown.apoli.condition.EntityCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record Skill(
    ResourceLocation id,
    ResourceLocation parent,
    Component name,
    Component description,
    ItemStack icon,
    List<ResourceLocation> powers,
    Optional<EntityCondition> condition,
    Optional<EntityCondition> visibilityCondition,
    List<ResourceLocation> excludes,
    int cost,
    int order
) {
    public ResourceLocation rootId() {
        return SkillRegistry.rootOf(id);
    }
}
