package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BuySkillC2S(ResourceLocation skill) implements CustomPacketPayload {
    public static final Type<BuySkillC2S> TYPE = new Type<>(Apoli.id("buy_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuySkillC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeResourceLocation(payload.skill),
        buf -> new BuySkillC2S(buf.readResourceLocation()));

    @Override
    public Type<BuySkillC2S> type() {
        return TYPE;
    }
}
