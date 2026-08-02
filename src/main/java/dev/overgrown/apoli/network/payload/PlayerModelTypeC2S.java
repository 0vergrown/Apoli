package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.PlayerModelType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PlayerModelTypeC2S(PlayerModelType modelType) {
    public static final ResourceLocation CHANNEL = Apoli.id("player_model_type");

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(modelType == PlayerModelType.SLIM);
    }

    public static PlayerModelTypeC2S read(FriendlyByteBuf buf) {
        return new PlayerModelTypeC2S(buf.readBoolean() ? PlayerModelType.SLIM : PlayerModelType.WIDE);
    }
}
