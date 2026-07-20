package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public record LabelUpdateS2C(int entityId, Map<ResourceLocation, Component> texts) {
    public static final ResourceLocation CHANNEL = Apoli.id("label_update");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(texts.size());
        for (Map.Entry<ResourceLocation, Component> entry : texts.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeComponent(entry.getValue());
        }
    }

    public static LabelUpdateS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int count = buf.readVarInt();
        Map<ResourceLocation, Component> texts = new LinkedHashMap<>(Math.max(1, count));
        for (int i = 0; i < count; i++) {
            ResourceLocation powerId = buf.readResourceLocation();
            texts.put(powerId, buf.readComponent());
        }
        return new LabelUpdateS2C(entityId, texts);
    }
}
