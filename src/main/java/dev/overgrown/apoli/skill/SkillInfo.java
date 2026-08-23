package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.apoli.codec.IdCodecs;
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
        IdCodecs.ID.fieldOf("parent").forGetter(SkillInfo::parent),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(SkillInfo::condition),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("visibility_condition", EntityCondition.CODEC).forGetter(SkillInfo::visibilityCondition),
        IdCodecs.ID.listOf().optionalFieldOf("excludes", List.of()).forGetter(SkillInfo::excludes),
        Codec.INT.optionalFieldOf("cost", 0).forGetter(SkillInfo::cost),
        Codec.INT.optionalFieldOf("order", 0).forGetter(SkillInfo::order)
    ).apply(i, (icon, frame, parent, condition, visibilityCondition, excludes, cost, order) ->
        new SkillInfo(icon, frame, parent, condition, visibilityCondition, List.copyOf(excludes), cost, order)));
}
