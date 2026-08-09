package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.IconData;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record SkillInfo(
    IconData icon,
    SkillFrame frame,
    ResourceLocation parent,
    Optional<EntityCondition> condition,
    Optional<EntityCondition> visibilityCondition,
    List<ResourceLocation> excludes,
    int cost,
    int order
) {
    public static final Codec<SkillInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
        IconData.CODEC.optionalFieldOf("icon", IconData.grassBlock()).forGetter(SkillInfo::icon),
        SkillFrame.CODEC.optionalFieldOf("frame", SkillFrame.TASK).forGetter(SkillInfo::frame),
        ResourceLocation.CODEC.fieldOf("parent").forGetter(SkillInfo::parent),
        EntityCondition.CODEC.optionalFieldOf("condition").forGetter(SkillInfo::condition),
        EntityCondition.CODEC.optionalFieldOf("visibility_condition").forGetter(SkillInfo::visibilityCondition),
        ResourceLocation.CODEC.listOf().optionalFieldOf("excludes", List.of()).forGetter(SkillInfo::excludes),
        Codec.INT.optionalFieldOf("cost", 0).forGetter(SkillInfo::cost),
        Codec.INT.optionalFieldOf("order", 0).forGetter(SkillInfo::order)
    ).apply(i, (icon, frame, parent, condition, visibilityCondition, excludes, cost, order) ->
        new SkillInfo(icon, frame, parent, condition, visibilityCondition, List.copyOf(excludes), cost, order)));
}
