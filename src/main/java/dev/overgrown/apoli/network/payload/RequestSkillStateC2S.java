package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.resources.ResourceLocation;

public final class RequestSkillStateC2S {
    public static final ResourceLocation CHANNEL = Apoli.id("request_skill_state");

    private RequestSkillStateC2S() {}
}
