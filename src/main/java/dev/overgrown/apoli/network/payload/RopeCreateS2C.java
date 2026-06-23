package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record RopeCreateS2C(UUID owner, Vec3 anchor, double length, float maxLength, ResourceLocation texture) {
    public static final ResourceLocation CHANNEL = Apoli.id("rope_create");

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeDouble(anchor.x);
        buf.writeDouble(anchor.y);
        buf.writeDouble(anchor.z);
        buf.writeDouble(length);
        buf.writeFloat(maxLength);
        buf.writeResourceLocation(texture);
    }

    public static RopeCreateS2C read(FriendlyByteBuf buf) {
        UUID owner = buf.readUUID();
        Vec3 anchor = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new RopeCreateS2C(owner, anchor, buf.readDouble(), buf.readFloat(), buf.readResourceLocation());
    }
}
