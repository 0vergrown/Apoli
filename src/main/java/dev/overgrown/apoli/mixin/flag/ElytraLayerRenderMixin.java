package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ElytraFlightPower;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ElytraLayer.class)
@OnlyIn(Dist.CLIENT)
public abstract class ElytraLayerRenderMixin {
    @Unique
    private LivingEntity apoli$entity;

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void apoli$forceElytraRender(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        this.apoli$entity = entity;
        if (!entity.isInvisible() && apoli$shouldRenderElytra(entity)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private ResourceLocation apoli$elytraTexture(ResourceLocation original) {
        ResourceLocation[] texture = new ResourceLocation[]{null};
        if (apoli$entity != null) {
            PowerLookup.forEach(apoli$entity, Apoli.id("elytra_flight"), ElytraFlightPower.Config.class, cfg -> {
                if (texture[0] == null) cfg.textureLocation().ifPresent(t -> texture[0] = t);
            });
        }
        return texture[0] != null ? texture[0] : original;
    }

    @Unique
    private static boolean apoli$shouldRenderElytra(LivingEntity entity) {
        boolean[] render = new boolean[]{false};
        PowerLookup.forEach(entity, Apoli.id("elytra_flight"), ElytraFlightPower.Config.class, cfg -> {
            if (cfg.renderElytra()) render[0] = true;
        });
        return render[0];
    }
}
