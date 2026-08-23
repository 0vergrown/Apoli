package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
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
    int cost,
    int order
) {
    public ResourceLocation rootId() {
        return SkillRegistry.rootOf(id);
    }

    public static String translationKey(ResourceLocation id, String suffix) {
        return "skill." + id.getNamespace() + "." + id.getPath().replace('/', '.') + "." + suffix;
    }

    public static MapCodec<Skill> fileCodec(ResourceLocation id) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("parent").forGetter(Skill::parent),
            TextComponent.CODEC.optionalFieldOf("name", Component.translatable(translationKey(id, "name"))).forGetter(Skill::name),
            TextComponent.CODEC.optionalFieldOf("description", Component.translatable(translationKey(id, "description"))).forGetter(Skill::description),
            IconData.CODEC.optionalFieldOf("icon", IconData.grassBlock()).forGetter(Skill::icon),
            SkillFrame.CODEC.optionalFieldOf("frame", SkillFrame.TASK).forGetter(Skill::frame),
            SingleOrList.of(IdCodecs.ID).optionalFieldOf("powers", List.of()).forGetter(Skill::powers),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(Skill::condition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("visibility_condition", EntityCondition.CODEC).forGetter(Skill::visibilityCondition),
            IdCodecs.ID.listOf().optionalFieldOf("excludes", List.of()).forGetter(Skill::excludes),
            Codec.INT.optionalFieldOf("cost", 0).forGetter(Skill::cost),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Skill::order)
        ).apply(i, (parent, name, description, icon, frame, powers, condition, visibilityCondition, excludes, cost, order) ->
            new Skill(id, parent, name, description, icon, frame, List.copyOf(powers), condition,
                visibilityCondition, List.copyOf(excludes), cost, order)));
    }
}
