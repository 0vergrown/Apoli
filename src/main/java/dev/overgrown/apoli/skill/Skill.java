package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    public static String translationKey(ResourceLocation id, String suffix) {
        return "skill." + id.getNamespace() + "." + id.getPath().replace('/', '.') + "." + suffix;
    }

    public static MapCodec<Skill> fileCodec(ResourceLocation id) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("parent").forGetter(Skill::parent),
            TextComponent.CODEC.optionalFieldOf("name", Component.translatable(translationKey(id, "name"))).forGetter(Skill::name),
            TextComponent.CODEC.optionalFieldOf("description", Component.translatable(translationKey(id, "description"))).forGetter(Skill::description),
            ItemStackData.CODEC.optionalFieldOf("icon", new ItemStackData(new ItemStack(Items.GRASS_BLOCK))).forGetter(s -> new ItemStackData(s.icon())),
            SingleOrList.of(ResourceLocation.CODEC).optionalFieldOf("powers", List.of()).forGetter(Skill::powers),
            EntityCondition.CODEC.optionalFieldOf("condition").forGetter(Skill::condition),
            EntityCondition.CODEC.optionalFieldOf("visibility_condition").forGetter(Skill::visibilityCondition),
            ResourceLocation.CODEC.listOf().optionalFieldOf("excludes", List.of()).forGetter(Skill::excludes),
            Codec.INT.optionalFieldOf("cost", 0).forGetter(Skill::cost),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Skill::order)
        ).apply(i, (parent, name, description, icon, powers, condition, visibilityCondition, excludes, cost, order) ->
            new Skill(id, parent, name, description, icon.stack(), List.copyOf(powers), condition,
                visibilityCondition, List.copyOf(excludes), cost, order)));
    }
}
