package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SkillInfo(
    IconData icon,
    SkillFrame frame,
    ResourceLocation parent,
    Optional<EntityCondition> condition,
    Optional<EntityCondition> visibilityCondition,
    List<ResourceLocation> excludes,
    SkillCost cost,
    int order,
    SkillReward reward,
    Optional<SkillPlacement> placement
) {
    public static final Codec<SkillInfo> CODEC = AliasingMapCodec.wrap(RecordCodecBuilder.<SkillInfo>mapCodec(i -> i.group(
        IconData.CODEC.optionalFieldOf("icon", IconData.grassBlock()).forGetter(SkillInfo::icon),
        SkillFrame.CODEC.optionalFieldOf("frame", SkillFrame.TASK).forGetter(SkillInfo::frame),
        IdCodecs.ID.fieldOf("parent").forGetter(SkillInfo::parent),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(SkillInfo::condition),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("visibility_condition", EntityCondition.CODEC).forGetter(SkillInfo::visibilityCondition),
        IdCodecs.ID.listOf().optionalFieldOf("excludes", List.of()).forGetter(SkillInfo::excludes),
        SkillCost.CODEC.optionalFieldOf("cost", SkillCost.FREE).forGetter(SkillInfo::cost),
        Codec.INT.optionalFieldOf("order", 0).forGetter(SkillInfo::order),
        LoggedOptionalField.strict("reward", SkillReward.CODEC).forGetter(info -> Skill.optionalReward(info.reward())),
        LoggedOptionalField.strict("placement", SkillPlacement.CODEC).forGetter(SkillInfo::placement)
    ).apply(i, (icon, frame, parent, condition, visibilityCondition, excludes, cost, order, reward, placement) ->
        new SkillInfo(icon, frame, parent, condition, visibilityCondition, List.copyOf(excludes), cost, order,
            reward.orElse(SkillReward.NONE), placement))), Map.of("rewards", "reward")).codec();
}
