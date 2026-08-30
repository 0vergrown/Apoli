package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CameraPerspectiveC2S(boolean firstPerson) {
    public static final ResourceLocation CHANNEL = Apoli.id("camera_perspective");

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(firstPerson);
    }

    public static CameraPerspectiveC2S read(FriendlyByteBuf buf) {
        return new CameraPerspectiveC2S(buf.readBoolean());
    }
}
