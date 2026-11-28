package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

// The current value of every data pack gamerule.
public record GameRulesS2C(Map<ResourceLocation, Integer> values) implements CustomPacketPayload {
    public static final Type<GameRulesS2C> TYPE = new Type<>(Apoli.id("gamerules"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GameRulesS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        GameRulesS2C::read);
    private static final int MAX_RULES = 4096;

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(values.size());
        values.forEach((id, value) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(value);
        });
    }

    public static GameRulesS2C read(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_RULES);
        Map<ResourceLocation, Integer> values = new HashMap<>(count);
        for (int i = 0; i < count; i++) values.put(buf.readResourceLocation(), buf.readVarInt());
        return new GameRulesS2C(values);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
