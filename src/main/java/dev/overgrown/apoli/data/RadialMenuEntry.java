package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record RadialMenuEntry(
    ItemStack item,
    Optional<ResourceLocation> buttonTexture,
    Optional<ResourceLocation> icon,
    Optional<ResourceLocation> highlightIcon,
    Optional<ResourceLocation> highlightButtonTexture,
    EntityAction entityAction,
    Optional<EntityCondition> condition,
    int distance,
    int velocity,
    Optional<Component> tooltip,
    int buttonWidth,
    int buttonHeight,
    int iconWidth,
    int iconHeight,
    int itemWidth,
    int itemHeight,
    int offsetX,
    int offsetY,
    Optional<Float> angle
) {
    private record Placement(int offsetX, int offsetY, Optional<Float> angle) {}

    private static final MapCodec<RadialMenuEntry> BODY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemStackData.CODEC.optionalFieldOf("item").forGetter(e ->
            e.item.isEmpty() ? Optional.<ItemStackData>empty() : Optional.of(new ItemStackData(e.item))),
        IdCodecs.ID.optionalFieldOf("button_texture").forGetter(RadialMenuEntry::buttonTexture),
        IdCodecs.ID.optionalFieldOf("icon").forGetter(RadialMenuEntry::icon),
        IdCodecs.ID.optionalFieldOf("highlight_icon_texture").forGetter(RadialMenuEntry::highlightIcon),
        IdCodecs.ID.optionalFieldOf("highlight_button_texture").forGetter(RadialMenuEntry::highlightButtonTexture),
        EntityAction.CODEC.fieldOf("entity_action").forGetter(RadialMenuEntry::entityAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(RadialMenuEntry::condition),
        Codec.INT.optionalFieldOf("distance", -1).forGetter(RadialMenuEntry::distance),
        Codec.INT.optionalFieldOf("velocity", -1).forGetter(RadialMenuEntry::velocity),
        TextComponent.CODEC.optionalFieldOf("tooltip").forGetter(RadialMenuEntry::tooltip),
        Codec.INT.optionalFieldOf("button_width", 16).forGetter(RadialMenuEntry::buttonWidth),
        Codec.INT.optionalFieldOf("button_height", 20).forGetter(RadialMenuEntry::buttonHeight),
        Codec.INT.optionalFieldOf("icon_width", 16).forGetter(RadialMenuEntry::iconWidth),
        Codec.INT.optionalFieldOf("icon_height", 16).forGetter(RadialMenuEntry::iconHeight),
        Codec.INT.optionalFieldOf("item_width", 16).forGetter(RadialMenuEntry::itemWidth),
        Codec.INT.optionalFieldOf("item_height", 16).forGetter(RadialMenuEntry::itemHeight)
    ).apply(instance, RadialMenuEntry::fromData));

    private static final MapCodec<Placement> PLACEMENT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.optionalFieldOf("offset_x", 0).forGetter(Placement::offsetX),
        Codec.INT.optionalFieldOf("offset_y", 0).forGetter(Placement::offsetY),
        Codec.FLOAT.optionalFieldOf("angle").forGetter(Placement::angle)
    ).apply(instance, Placement::new));

    public static final Codec<RadialMenuEntry> CODEC = Codec.mapPair(BODY_CODEC, PLACEMENT_CODEC).xmap(
        pair -> pair.getFirst().withPlacement(pair.getSecond()),
        entry -> Pair.of(entry, new Placement(entry.offsetX, entry.offsetY, entry.angle))
    ).codec();

    private RadialMenuEntry withPlacement(Placement placement) {
        return new RadialMenuEntry(item, buttonTexture, icon, highlightIcon, highlightButtonTexture,
            entityAction, condition, distance, velocity, tooltip,
            buttonWidth, buttonHeight, iconWidth, iconHeight, itemWidth, itemHeight,
            placement.offsetX(), placement.offsetY(), placement.angle());
    }

    private static RadialMenuEntry fromData(
        Optional<ItemStackData> item,
        Optional<ResourceLocation> buttonTexture,
        Optional<ResourceLocation> icon,
        Optional<ResourceLocation> highlightIcon,
        Optional<ResourceLocation> highlightButtonTexture,
        EntityAction entityAction,
        Optional<EntityCondition> condition,
        int distance,
        int velocity,
        Optional<Component> tooltip,
        int buttonWidth,
        int buttonHeight,
        int iconWidth,
        int iconHeight,
        int itemWidth,
        int itemHeight
    ) {
        return new RadialMenuEntry(
            item.map(ItemStackData::stack).orElse(ItemStack.EMPTY),
            buttonTexture, icon, highlightIcon, highlightButtonTexture,
            entityAction, condition, distance, velocity, tooltip,
            buttonWidth, buttonHeight, iconWidth, iconHeight, itemWidth, itemHeight,
            0, 0, Optional.empty());
    }
}
