package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public record MouseMovementC2S(float yaw, float pitch) {
    public static final float MAX_DEGREES = 720.0F;
    public static final ResourceLocation CHANNEL = Apoli.id("mouse_movement");

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }

    public static MouseMovementC2S read(FriendlyByteBuf buf) {
        return new MouseMovementC2S(clamp(buf.readFloat()), clamp(buf.readFloat()));
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) return 0.0F;
        return Mth.clamp(value, -MAX_DEGREES, MAX_DEGREES);
    }
}
