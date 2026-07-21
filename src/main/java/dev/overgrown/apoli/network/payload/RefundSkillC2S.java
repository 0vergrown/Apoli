package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RefundSkillC2S(ResourceLocation skill) implements CustomPacketPayload {
    public static final Type<RefundSkillC2S> TYPE = new Type<>(Apoli.id("refund_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RefundSkillC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeResourceLocation(payload.skill),
        buf -> new RefundSkillC2S(buf.readResourceLocation()));

    @Override
    public Type<RefundSkillC2S> type() {
        return TYPE;
    }
}
