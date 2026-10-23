package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Skill(
    ResourceLocation id,
    ResourceLocation parent,
    Component name,
    Component description,
    IconData icon,
    SkillFrame frame,
    List<ResourceLocation> powers,
    Optional<EntityCondition> condition,
    Optional<EntityCondition> visibilityCondition,
    List<ResourceLocation> excludes,
    SkillCost cost,
    int order,
    SkillReward reward,
    Optional<SkillPlacement> placement
) {
    public ResourceLocation rootId() {
        return SkillRegistry.rootOf(id);
    }

    public boolean isGate() {
        return powers.isEmpty() && reward.isEmpty();
    }

    static Optional<SkillReward> optionalReward(SkillReward reward) {
        return reward.isEmpty() && !reward.hidden() ? Optional.empty() : Optional.of(reward);
    }

    public static String translationKey(ResourceLocation id, String suffix) {
        return "skill." + id.getNamespace() + "." + id.getPath().replace('/', '.') + "." + suffix;
    }

    public static MapCodec<Skill> fileCodec(ResourceLocation id) {
        return AliasingMapCodec.wrap(RecordCodecBuilder.<Skill>mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("parent").forGetter(Skill::parent),
            TextComponent.CODEC.optionalFieldOf("name", Component.translatable(translationKey(id, "name"))).forGetter(Skill::name),
            TextComponent.CODEC.optionalFieldOf("description", Component.translatable(translationKey(id, "description"))).forGetter(Skill::description),
            IconData.CODEC.optionalFieldOf("icon", IconData.grassBlock()).forGetter(Skill::icon),
            SkillFrame.CODEC.optionalFieldOf("frame", SkillFrame.TASK).forGetter(Skill::frame),
            SingleOrList.of(IdCodecs.ID).optionalFieldOf("powers", List.of()).forGetter(Skill::powers),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(Skill::condition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("visibility_condition", EntityCondition.CODEC).forGetter(Skill::visibilityCondition),
            IdCodecs.ID.listOf().optionalFieldOf("excludes", List.of()).forGetter(Skill::excludes),
            SkillCost.CODEC.optionalFieldOf("cost", SkillCost.FREE).forGetter(Skill::cost),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Skill::order),
            LoggedOptionalField.strict("reward", SkillReward.CODEC).forGetter(skill -> optionalReward(skill.reward())),
            LoggedOptionalField.strict("placement", SkillPlacement.CODEC).forGetter(Skill::placement)
        ).apply(i, (parent, name, description, icon, frame, powers, condition, visibilityCondition, excludes, cost, order, reward, placement) ->
            new Skill(id, parent, name, description, icon, frame, List.copyOf(powers), condition,
                visibilityCondition, List.copyOf(excludes), cost, order, reward.orElse(SkillReward.NONE), placement))),
            Map.of("rewards", "reward"));
    }
}
