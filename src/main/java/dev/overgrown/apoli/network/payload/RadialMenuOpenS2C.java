package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record RadialMenuOpenS2C(int nonce, Optional<ResourceLocation> sprite, List<Entry> entries) implements CustomPacketPayload {

    public record Entry(
        ItemStack item,
        Optional<ResourceLocation> buttonTexture,
        Optional<ResourceLocation> icon,
        Optional<ResourceLocation> highlightIcon,
        Optional<ResourceLocation> highlightButtonTexture,
        Optional<Component> tooltip,
        int distance,
        int velocity,
        int buttonWidth,
        int buttonHeight,
        int iconWidth,
        int iconHeight,
        int itemWidth,
        int itemHeight
    ) {}

    public static final Type<RadialMenuOpenS2C> TYPE = new Type<>(Apoli.id("radial_menu_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadialMenuOpenS2C> STREAM_CODEC =
        StreamCodec.of(RadialMenuOpenS2C::write, RadialMenuOpenS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, RadialMenuOpenS2C payload) {
        buf.writeVarInt(payload.nonce);
        buf.writeOptional(payload.sprite, (b, v) -> b.writeResourceLocation(v));
        buf.writeVarInt(payload.entries.size());
        for (Entry e : payload.entries) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.item);
            buf.writeOptional(e.buttonTexture, (b, v) -> b.writeResourceLocation(v));
            buf.writeOptional(e.icon, (b, v) -> b.writeResourceLocation(v));
            buf.writeOptional(e.highlightIcon, (b, v) -> b.writeResourceLocation(v));
            buf.writeOptional(e.highlightButtonTexture, (b, v) -> b.writeResourceLocation(v));
            if (e.tooltip.isPresent()) {
                buf.writeBoolean(true);
                ComponentSerialization.STREAM_CODEC.encode(buf, e.tooltip.get());
            } else {
                buf.writeBoolean(false);
            }
            buf.writeInt(e.distance);
            buf.writeInt(e.velocity);
            buf.writeVarInt(e.buttonWidth);
            buf.writeVarInt(e.buttonHeight);
            buf.writeVarInt(e.iconWidth);
            buf.writeVarInt(e.iconHeight);
            buf.writeVarInt(e.itemWidth);
            buf.writeVarInt(e.itemHeight);
        }
    }

    private static RadialMenuOpenS2C read(RegistryFriendlyByteBuf buf) {
        int nonce = buf.readVarInt();
        Optional<ResourceLocation> sprite = buf.readOptional(b -> b.readResourceLocation());
        int n = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(Math.min(n, 256));
        for (int i = 0; i < n; i++) {
            ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            Optional<ResourceLocation> buttonTexture = buf.readOptional(b -> b.readResourceLocation());
            Optional<ResourceLocation> icon = buf.readOptional(b -> b.readResourceLocation());
            Optional<ResourceLocation> highlightIcon = buf.readOptional(b -> b.readResourceLocation());
            Optional<ResourceLocation> highlightButtonTexture = buf.readOptional(b -> b.readResourceLocation());
            Optional<Component> tooltip = buf.readBoolean()
                ? Optional.of(ComponentSerialization.STREAM_CODEC.decode(buf))
                : Optional.empty();
            int distance = buf.readInt();
            int velocity = buf.readInt();
            int buttonWidth = buf.readVarInt();
            int buttonHeight = buf.readVarInt();
            int iconWidth = buf.readVarInt();
            int iconHeight = buf.readVarInt();
            int itemWidth = buf.readVarInt();
            int itemHeight = buf.readVarInt();
            entries.add(new Entry(item, buttonTexture, icon, highlightIcon, highlightButtonTexture,
                tooltip, distance, velocity, buttonWidth, buttonHeight, iconWidth, iconHeight, itemWidth, itemHeight));
        }
        return new RadialMenuOpenS2C(nonce, sprite, entries);
    }

    @Override
    public Type<RadialMenuOpenS2C> type() {
        return TYPE;
    }
}
