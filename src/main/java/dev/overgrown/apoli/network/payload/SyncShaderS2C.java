package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record SyncShaderS2C(@Nullable ResourceLocation shader, boolean toggleable) implements CustomPacketPayload {
    public static final Type<SyncShaderS2C> TYPE = new Type<>(Apoli.id("sync_shader"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncShaderS2C> STREAM_CODEC = StreamCodec.of(
        SyncShaderS2C::write,
        SyncShaderS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, SyncShaderS2C payload) {
        buf.writeBoolean(payload.shader != null);
        if (payload.shader != null) buf.writeResourceLocation(payload.shader);
        buf.writeBoolean(payload.toggleable);
    }

    private static SyncShaderS2C read(RegistryFriendlyByteBuf buf) {
        ResourceLocation shader = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new SyncShaderS2C(shader, buf.readBoolean());
    }

    @Override
    public Type<SyncShaderS2C> type() {
        return TYPE;
    }
}
