package dev.overgrown.apoli.mixin.shader;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererShaderAccessor {

    @Invoker("loadEffect")
    void apoli$invokeLoadEffect(ResourceLocation shader);

    @Accessor("postEffect")
    PostChain apoli$getPostEffect();
}
