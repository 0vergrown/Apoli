package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record BuySkillC2S(ResourceLocation skill) {
    public static final ResourceLocation CHANNEL = Apoli.id("buy_skill");

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(skill);
    }

    public static BuySkillC2S read(FriendlyByteBuf buf) {
        return new BuySkillC2S(buf.readResourceLocation());
    }
}
