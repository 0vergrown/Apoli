package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RefundSkillC2S(ResourceLocation skill) {
    public static final ResourceLocation CHANNEL = Apoli.id("refund_skill");

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(skill);
    }

    public static RefundSkillC2S read(FriendlyByteBuf buf) {
        return new RefundSkillC2S(buf.readResourceLocation());
    }
}
