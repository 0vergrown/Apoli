package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Skill(
    ResourceLocation id,
    Optional<ResourceLocation> parent,
    Component name,
    Component description,
    ItemStack icon,
    List<ResourceLocation> powers,
    List<ResourceLocation> defaultPowers,
    Optional<ResourceLocation> background,
    Optional<EntityCondition> condition,
    Optional<EntityCondition> visibilityCondition,
    List<ResourceLocation> excludes,
    int cost,
    int order
) {
    public boolean isRoot() {
        return parent.isEmpty();
    }

    public ResourceLocation rootId() {
        return SkillRegistry.rootOf(id);
    }

    public static MapCodec<Skill> codec(ResourceLocation id) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("parent").forGetter(Skill::parent),
            TextComponent.CODEC.optionalFieldOf("name", Component.empty()).forGetter(Skill::name),
            TextComponent.CODEC.optionalFieldOf("description", Component.empty()).forGetter(Skill::description),
            ItemStackData.CODEC.optionalFieldOf("icon", new ItemStackData(new ItemStack(Items.GRASS_BLOCK))).forGetter(s -> new ItemStackData(s.icon())),
            ResourceLocation.CODEC.optionalFieldOf("power").forGetter(s -> Optional.empty()),
            ResourceLocation.CODEC.listOf().optionalFieldOf("powers", List.of()).forGetter(Skill::powers),
            ResourceLocation.CODEC.listOf().optionalFieldOf("default_powers", List.of()).forGetter(Skill::defaultPowers),
            ResourceLocation.CODEC.optionalFieldOf("background").forGetter(Skill::background),
            
            EntityCondition.CODEC.optionalFieldOf("condition").forGetter(Skill::condition),
            
            EntityCondition.CODEC.optionalFieldOf("visibility_condition").forGetter(Skill::visibilityCondition),
            
            ResourceLocation.CODEC.listOf().optionalFieldOf("excludes", List.of()).forGetter(Skill::excludes),
            Codec.INT.optionalFieldOf("cost", 0).forGetter(Skill::cost),
            
            Codec.INT.optionalFieldOf("order", 0).forGetter(Skill::order)
        ).apply(i, (parent, name, description, icon, power, powers, defaultPowers, background, condition, visibilityCondition, excludes, cost, order) -> {
            List<ResourceLocation> allPowers = new ArrayList<>(powers);
            power.ifPresent(allPowers::add);
            return new Skill(id, parent, name, description, icon.stack(), List.copyOf(allPowers), defaultPowers,
                background, condition, visibilityCondition, List.copyOf(excludes), cost, order);
        }));
    }
}
