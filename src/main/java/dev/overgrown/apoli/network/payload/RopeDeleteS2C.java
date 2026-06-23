package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record RopeDeleteS2C(UUID owner) {
    public static final ResourceLocation CHANNEL = Apoli.id("rope_delete");

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
    }

    public static RopeDeleteS2C read(FriendlyByteBuf buf) {
        return new RopeDeleteS2C(buf.readUUID());
    }
}
