package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ElytraFlightPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ElytraLayer.class)
@Environment(EnvType.CLIENT)
public abstract class ElytraLayerRenderMixin {
    @Unique
    private LivingEntity apoli$entity;

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean apoli$forceElytraRender(boolean original, @Local(argsOnly = true) LivingEntity entity) {
        this.apoli$entity = entity;
        return original || (!entity.isInvisible() && ElytraFlightPower.shouldRenderElytra(entity));
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private ResourceLocation apoli$elytraTexture(ResourceLocation original) {
        ResourceLocation texture = ElytraFlightPower.textureOf(apoli$entity);
        return texture != null ? texture : original;
    }
}
