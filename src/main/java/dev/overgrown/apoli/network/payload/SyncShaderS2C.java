package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record SyncShaderS2C(@Nullable ResourceLocation shader, boolean toggleable) {
    public static final ResourceLocation CHANNEL = Apoli.id("sync_shader");

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(shader != null);
        if (shader != null) buf.writeResourceLocation(shader);
        buf.writeBoolean(toggleable);
    }

    public static SyncShaderS2C read(FriendlyByteBuf buf) {
        ResourceLocation shader = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new SyncShaderS2C(shader, buf.readBoolean());
    }
}
