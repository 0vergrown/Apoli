package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CameraTypeC2S(int cameraType) {
    public static final ResourceLocation CHANNEL = Apoli.id("camera_type");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(cameraType);
    }

    public static CameraTypeC2S read(FriendlyByteBuf buf) {
        return new CameraTypeC2S(buf.readVarInt());
    }
}
