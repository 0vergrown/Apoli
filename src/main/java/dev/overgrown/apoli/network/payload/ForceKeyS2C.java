package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ForceKeyS2C(String key, int duration, boolean release) {
    public static final ResourceLocation CHANNEL = Apoli.id("force_key");

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(key);
        buf.writeVarInt(duration);
        buf.writeBoolean(release);
    }

    public static ForceKeyS2C read(FriendlyByteBuf buf) {
        String key = buf.readUtf();
        int duration = buf.readVarInt();
        boolean release = buf.readBoolean();
        return new ForceKeyS2C(key, duration, release);
    }
}
