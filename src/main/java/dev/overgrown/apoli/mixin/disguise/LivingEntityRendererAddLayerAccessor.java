package dev.overgrown.apoli.mixin.disguise;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public interface LivingEntityRendererAddLayerAccessor {

    @Invoker("addLayer")
    boolean apoli$addLayer(RenderLayer<?, ?> layer);
}
