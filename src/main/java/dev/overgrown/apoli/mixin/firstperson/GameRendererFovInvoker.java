package dev.overgrown.apoli.mixin.firstperson;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public interface GameRendererFovInvoker {

    @Invoker("getFov")
    double apoli$getFov(Camera camera, float partialTick, boolean useFovSetting);
}
