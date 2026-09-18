package dev.overgrown.apoli.mixin.custom_model;

import dev.overgrown.apoli.client.render.LivingCustomModelLayer;
import dev.overgrown.apoli.client.summon.CloneRenderer;
import dev.overgrown.apoli.mixin.disguise.LivingEntityRendererAddLayerAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LivingEntityRendererCustomModelMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void apoli$addCustomModelLayer(EntityRendererProvider.Context context, M model, float shadowRadius,
                                           CallbackInfo ci) {
        Object self = this;
        if (self instanceof PlayerRenderer || self instanceof CloneRenderer) return;
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<T, M> renderer = (LivingEntityRenderer<T, M>) self;
        ((LivingEntityRendererAddLayerAccessor) self).apoli$addLayer(new LivingCustomModelLayer<>(renderer));
    }
}
