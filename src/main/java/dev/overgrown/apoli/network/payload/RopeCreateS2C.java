package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.rope.RopeAnchor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record RopeCreateS2C(int id, RopeAnchor from, RopeAnchor to, double length, float maxLength,
                            ResourceLocation texture, @Nullable UUID owner, boolean controllable) {
    public static final ResourceLocation CHANNEL = Apoli.id("rope_create");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        from.write(buf);
        to.write(buf);
        buf.writeDouble(length);
        buf.writeFloat(maxLength);
        buf.writeResourceLocation(texture);
        buf.writeBoolean(owner != null);
        if (owner != null) buf.writeUUID(owner);
        buf.writeBoolean(controllable);
    }

    public static RopeCreateS2C read(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        RopeAnchor from = RopeAnchor.read(buf);
        RopeAnchor to = RopeAnchor.read(buf);
        double length = buf.readDouble();
        float maxLength = buf.readFloat();
        ResourceLocation texture = buf.readResourceLocation();
        UUID owner = buf.readBoolean() ? buf.readUUID() : null;
        boolean controllable = buf.readBoolean();
        return new RopeCreateS2C(id, from, to, length, maxLength, texture, owner, controllable);
    }
}
