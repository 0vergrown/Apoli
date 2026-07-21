package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

public record SkillTree(
    ResourceLocation id,
    Component name,
    Component description,
    ItemStack icon,
    Optional<ResourceLocation> background,
    List<ResourceLocation> defaultPowers,
    int order,
    boolean autoGrant,
    boolean refundable
) {
    public static MapCodec<SkillTree> codec(ResourceLocation id) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TextComponent.CODEC.optionalFieldOf("name", Component.empty()).forGetter(SkillTree::name),
            TextComponent.CODEC.optionalFieldOf("description", Component.empty()).forGetter(SkillTree::description),
            ItemStackData.CODEC.optionalFieldOf("icon", new ItemStackData(new ItemStack(Items.GRASS_BLOCK))).forGetter(t -> new ItemStackData(t.icon())),
            ResourceLocation.CODEC.optionalFieldOf("background").forGetter(SkillTree::background),
            ResourceLocation.CODEC.listOf().optionalFieldOf("default_powers", List.of()).forGetter(SkillTree::defaultPowers),
            Codec.INT.optionalFieldOf("order", 0).forGetter(SkillTree::order),
            Codec.BOOL.optionalFieldOf("auto_grant", true).forGetter(SkillTree::autoGrant),
            Codec.BOOL.optionalFieldOf("refundable", true).forGetter(SkillTree::refundable)
        ).apply(i, (name, description, icon, background, defaultPowers, order, autoGrant, refundable) ->
            new SkillTree(id, name, description, icon.stack(), background, List.copyOf(defaultPowers), order, autoGrant, refundable)));
    }
}
