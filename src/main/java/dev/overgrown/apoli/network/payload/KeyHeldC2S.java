package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record KeyHeldC2S(List<String> keys) {
    public static final int MAX_KEYS = 64;
    public static final ResourceLocation CHANNEL = Apoli.id("key_held");

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
}
