package dev.overgrown.apoli.mixin.custom_model;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.render.CustomModelManager;
import dev.overgrown.apoli.client.render.DynamicTextures;
import dev.overgrown.apoli.client.render.LivingCustomModelLayer;
import dev.overgrown.apoli.client.summon.CloneRenderer;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import dev.overgrown.apoli.mixin.disguise.LivingEntityRendererAddLayerAccessor;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
@OnlyIn(Dist.CLIENT)
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

    @ModifyExpressionValue(method = "getRenderType",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation apoli$replaceTexture(ResourceLocation original, T entity) {
        if (entity instanceof Player) return original;
        CustomModelRenderPower.Config cfg = CustomModelRenderPower.firstReplace(entity);
        if (cfg == null) return original;
        return DynamicTextures.resolve(cfg.wide(), entity);
    }

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private RenderType apoli$hideReplacedModel(RenderType original, T entity, boolean bodyVisible,
                                               boolean translucent, boolean glowing) {
        if (original == null || entity instanceof Player || entity instanceof MinionEntity) return original;
        return CustomModelRenderPower.replacesModel(entity, id -> CustomModelManager.get(id) != null) ? null : original;
    }
}
