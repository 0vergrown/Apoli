package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record SkillTree(
    ResourceLocation id,
    Component name,
    Component description,
    IconData icon,
    SkillFrame frame,
    Optional<ResourceLocation> background,
    List<ResourceLocation> defaultPowers,
    int order,
    boolean autoGrant,
    boolean refundable,
    Optional<EntityCondition> condition
) {
    public static MapCodec<SkillTree> codec(ResourceLocation id) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TextComponent.CODEC.optionalFieldOf("name", Component.empty()).forGetter(SkillTree::name),
            TextComponent.CODEC.optionalFieldOf("description", Component.empty()).forGetter(SkillTree::description),
            IconData.CODEC.optionalFieldOf("icon", IconData.grassBlock()).forGetter(SkillTree::icon),
            SkillFrame.CODEC.optionalFieldOf("frame", SkillFrame.TASK).forGetter(SkillTree::frame),
            IdCodecs.ID.optionalFieldOf("background").forGetter(SkillTree::background),
            IdCodecs.ID.listOf().optionalFieldOf("default_powers", List.of()).forGetter(SkillTree::defaultPowers),
            Codec.INT.optionalFieldOf("order", 0).forGetter(SkillTree::order),
            Codec.BOOL.optionalFieldOf("auto_grant", true).forGetter(SkillTree::autoGrant),
            Codec.BOOL.optionalFieldOf("refundable", true).forGetter(SkillTree::refundable),
            EntityCondition.CODEC.optionalFieldOf("condition").forGetter(SkillTree::condition)
        ).apply(i, (name, description, icon, frame, background, defaultPowers, order, autoGrant, refundable, condition) ->
            new SkillTree(id, name, description, icon, frame, background, List.copyOf(defaultPowers), order,
                autoGrant, refundable, condition)));
    }
}
