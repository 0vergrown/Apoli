package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.ItemStackData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

public record SkillInfo(
    ItemStack icon,
    Optional<ResourceLocation> parent,
    Optional<ResourceLocation> background,
    Optional<EntityCondition> condition,
    Optional<EntityCondition> visibilityCondition,
    java.util.List<ResourceLocation> excludes,
    int cost,
    int order
) {
    public static final Codec<SkillInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
        ItemStackData.CODEC.optionalFieldOf("icon", new ItemStackData(new ItemStack(Items.GRASS_BLOCK))).forGetter(s -> new ItemStackData(s.icon())),
        ResourceLocation.CODEC.optionalFieldOf("parent").forGetter(SkillInfo::parent),
        ResourceLocation.CODEC.optionalFieldOf("background").forGetter(SkillInfo::background),
        EntityCondition.CODEC.optionalFieldOf("condition").forGetter(SkillInfo::condition),
        EntityCondition.CODEC.optionalFieldOf("visibility_condition").forGetter(SkillInfo::visibilityCondition),
        ResourceLocation.CODEC.listOf().optionalFieldOf("excludes", java.util.List.of()).forGetter(SkillInfo::excludes),
        Codec.INT.optionalFieldOf("cost", 0).forGetter(SkillInfo::cost),
        Codec.INT.optionalFieldOf("order", 0).forGetter(SkillInfo::order)
    ).apply(i, (icon, parent, background, condition, visibilityCondition, excludes, cost, order) ->
        new SkillInfo(icon.stack(), parent, background, condition, visibilityCondition, java.util.List.copyOf(excludes), cost, order)));
}
