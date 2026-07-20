package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public record LabelUpdateS2C(int entityId, Map<ResourceLocation, Component> texts) implements CustomPacketPayload {
    public static final Type<LabelUpdateS2C> TYPE = new Type<>(Apoli.id("label_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LabelUpdateS2C> STREAM_CODEC = StreamCodec.of(
        LabelUpdateS2C::write,
        LabelUpdateS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, LabelUpdateS2C payload) {
        buf.writeVarInt(payload.entityId);
        buf.writeVarInt(payload.texts.size());
        for (Map.Entry<ResourceLocation, Component> entry : payload.texts.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, entry.getValue());
        }
    }

    private static LabelUpdateS2C read(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int count = buf.readVarInt();
        Map<ResourceLocation, Component> texts = new LinkedHashMap<>(Math.max(1, count));
        for (int i = 0; i < count; i++) {
            ResourceLocation powerId = buf.readResourceLocation();
            texts.put(powerId, ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf));
        }
        return new LabelUpdateS2C(entityId, texts);
    }

    @Override
    public Type<LabelUpdateS2C> type() {
        return TYPE;
    }
}
