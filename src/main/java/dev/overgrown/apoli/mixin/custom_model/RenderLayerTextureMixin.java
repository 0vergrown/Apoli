package dev.overgrown.apoli.mixin.custom_model;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.render.DynamicTextures;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderLayer.class)
@OnlyIn(Dist.CLIENT)
public abstract class RenderLayerTextureMixin<T extends Entity, M extends EntityModel<T>> {
    @ModifyReturnValue(method = "getTextureLocation", at = @At("RETURN"))
    private ResourceLocation apoli$replaceLayerTexture(ResourceLocation original, T entity) {
        if (!(entity instanceof LivingEntity living) || entity instanceof Player) return original;
        CustomModelRenderPower.Config cfg = CustomModelRenderPower.firstReplace(living);
        if (cfg == null) return original;
        return DynamicTextures.resolve(cfg.wide(), living);
    }
}
