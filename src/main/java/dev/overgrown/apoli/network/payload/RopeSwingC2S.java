package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record RopeSwingC2S(int ropeId, Vec3 inputDir) {
    public static final ResourceLocation CHANNEL = Apoli.id("rope_swing");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(ropeId);
        buf.writeDouble(inputDir.x);
        buf.writeDouble(inputDir.y);
        buf.writeDouble(inputDir.z);
    }

    public static RopeSwingC2S read(FriendlyByteBuf buf) {
        return new RopeSwingC2S(buf.readVarInt(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }
}
