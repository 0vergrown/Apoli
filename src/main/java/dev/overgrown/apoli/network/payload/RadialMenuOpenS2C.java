package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record RadialMenuOpenS2C(int nonce, Optional<ResourceLocation> sprite, List<Entry> entries) {

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

    public static final ResourceLocation CHANNEL = Apoli.id("radial_menu_open");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(nonce);
        buf.writeOptional(sprite, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeItem(e.item());
            buf.writeOptional(e.buttonTexture(), FriendlyByteBuf::writeResourceLocation);
            buf.writeOptional(e.icon(), FriendlyByteBuf::writeResourceLocation);
            buf.writeOptional(e.highlightIcon(), FriendlyByteBuf::writeResourceLocation);
            buf.writeOptional(e.highlightButtonTexture(), FriendlyByteBuf::writeResourceLocation);
            buf.writeOptional(e.tooltip(), FriendlyByteBuf::writeComponent);
            buf.writeInt(e.distance());
            buf.writeInt(e.velocity());
            buf.writeVarInt(e.buttonWidth());
            buf.writeVarInt(e.buttonHeight());
            buf.writeVarInt(e.iconWidth());
            buf.writeVarInt(e.iconHeight());
            buf.writeVarInt(e.itemWidth());
            buf.writeVarInt(e.itemHeight());
        }
    }

    public static RadialMenuOpenS2C read(FriendlyByteBuf buf) {
        int nonce = buf.readVarInt();
        Optional<ResourceLocation> sprite = buf.readOptional(FriendlyByteBuf::readResourceLocation);
        int n = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(Math.min(n, 256));
        for (int i = 0; i < n; i++) {
            ItemStack item = buf.readItem();
            Optional<ResourceLocation> buttonTexture = buf.readOptional(FriendlyByteBuf::readResourceLocation);
            Optional<ResourceLocation> icon = buf.readOptional(FriendlyByteBuf::readResourceLocation);
            Optional<ResourceLocation> highlightIcon = buf.readOptional(FriendlyByteBuf::readResourceLocation);
            Optional<ResourceLocation> highlightButtonTexture = buf.readOptional(FriendlyByteBuf::readResourceLocation);
            Optional<Component> tooltip = buf.readOptional(FriendlyByteBuf::readComponent);
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
}
