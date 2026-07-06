package dev.overgrown.apoli.mixin.texture_overlay;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.client.render.OverlayRenderTypes;
import dev.overgrown.apoli.client.render.PlayerTextureOverlayLayer;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.EntityTextureOverlayPower;
import dev.overgrown.apoli.power.builtin.EntityTextureOverlayPower.ResolvedLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerRendererTextureOverlayMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    @Unique
    private boolean apoli$slim;

    protected PlayerRendererTextureOverlayMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadow) {
        super(ctx, model, shadow);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void apoli$addOverlayLayer(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        this.apoli$slim = slim;
        this.addLayer(new PlayerTextureOverlayLayer(this, slim));
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void apoli$replaceSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        EntityTextureOverlayPower.Config cfg = EntityTextureOverlayPower.firstReplace(player);
        if (cfg == null) return;
        cir.setReturnValue(apoli$slim ? cfg.slim() : cfg.wide());
    }

    @ModifyExpressionValue(method = "renderHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkinTextureLocation()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation apoli$replaceHandSkin(ResourceLocation original, PoseStack pose, MultiBufferSource buffers,
                                                   int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve) {
        EntityTextureOverlayPower.Config cfg = EntityTextureOverlayPower.firstReplace(player);
        if (cfg == null || !cfg.showFirstPerson()) return original;
        return apoli$slim ? cfg.slim() : cfg.wide();
    }

    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void apoli$rightArmOverlay(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        apoli$renderArmOverlay(pose, buffers, light, player, this.getModel().rightArm, this.getModel().rightSleeve);
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void apoli$leftArmOverlay(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        apoli$renderArmOverlay(pose, buffers, light, player, this.getModel().leftArm, this.getModel().leftSleeve);
    }

    @Unique
    private void apoli$renderArmOverlay(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                        ModelPart arm, ModelPart sleeve) {
        List<ResolvedLayer> layers = EntityTextureOverlayPower.collectLayers(player);
        if (layers.isEmpty()) return;
        PlayerModel<AbstractClientPlayer> model = this.getModel();
        for (ResolvedLayer layer : layers) {
            if (!layer.showFirstPerson() || !apoli$layerAffectsArm(model, layer, arm, sleeve)) continue;
            ResourceLocation texture = layer.texture(apoli$slim);
            VertexConsumer consumer = buffers.getBuffer(OverlayRenderTypes.forMode(layer.mode(), texture));
            boolean scaled = layer.scale() != 1.0F;
            if (scaled) {
                pose.pushPose();
                pose.scale(layer.scale(), layer.scale(), layer.scale());
            }
            arm.render(pose, consumer, light, OverlayTexture.NO_OVERLAY,
                layer.red(), layer.green(), layer.blue(), layer.alpha());
            sleeve.render(pose, consumer, light, OverlayTexture.NO_OVERLAY,
                layer.red(), layer.green(), layer.blue(), layer.alpha());
            if (scaled) pose.popPose();
        }
    }

    @Unique
    private boolean apoli$layerAffectsArm(PlayerModel<AbstractClientPlayer> model, ResolvedLayer layer, ModelPart arm, ModelPart sleeve) {
        if (layer.wholeModel()) return true;
        for (String partName : layer.bodyParts()) {
            List<ModelPart> parts = ModelPartLookup.resolve(model, ModelParts.normalize(partName));
            if (parts.contains(arm) || parts.contains(sleeve)) return true;
        }
        return false;
    }
}
