package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record KeyHeldC2S(List<String> keys) implements CustomPacketPayload {
    public static final int MAX_KEYS = 64;
    public static final Type<KeyHeldC2S> TYPE = new Type<>(Apoli.id("key_held"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KeyHeldC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        KeyHeldC2S::read);

    public void write(FriendlyByteBuf buf) {
        int n = Math.min(keys.size(), MAX_KEYS);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) buf.writeUtf(keys.get(i));
    }

    public static KeyHeldC2S read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> out = new ArrayList<>(Math.min(Math.max(n, 0), MAX_KEYS));
        for (int i = 0; i < n; i++) {
            String key = buf.readUtf();
            if (out.size() < MAX_KEYS) out.add(key);
        }
        return new KeyHeldC2S(out);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
