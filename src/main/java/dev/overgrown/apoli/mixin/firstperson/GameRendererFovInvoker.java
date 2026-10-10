package dev.overgrown.apoli.mixin.firstperson;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
@OnlyIn(Dist.CLIENT)
public interface GameRendererFovInvoker {

    @Invoker("getFov")
    double apoli$getFov(Camera camera, float partialTick, boolean useFovSetting);
}
