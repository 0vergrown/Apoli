package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestSkillStateC2S() implements CustomPacketPayload {
    public static final RequestSkillStateC2S INSTANCE = new RequestSkillStateC2S();
    public static final Type<RequestSkillStateC2S> TYPE = new Type<>(Apoli.id("request_skill_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSkillStateC2S> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public Type<RequestSkillStateC2S> type() {
        return TYPE;
    }
}
