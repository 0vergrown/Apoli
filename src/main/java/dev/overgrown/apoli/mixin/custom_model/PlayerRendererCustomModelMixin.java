package dev.overgrown.apoli.mixin.custom_model;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.client.render.AnimationPlayer;
import dev.overgrown.apoli.client.render.CustomModelManager;
import dev.overgrown.apoli.client.render.CustomModelRenderLayer;
import dev.overgrown.apoli.client.render.GeometryRenderer;
import dev.overgrown.apoli.client.render.GhostArmState;
import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.client.render.OverlayRenderTypes;
import dev.overgrown.apoli.client.render.PlayerRestPose;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.ResolvedLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class PlayerRendererCustomModelMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    @Unique
    private boolean apoli$slim;

    protected PlayerRendererCustomModelMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadow) {
        super(ctx, model, shadow);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void apoli$addCustomModelLayer(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        this.apoli$slim = slim;
        this.addLayer(new CustomModelRenderLayer(this, slim));
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void apoli$replaceSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        CustomModelRenderPower.Config cfg = CustomModelRenderPower.firstReplace(player);
        if (cfg == null) {
            return;
        }
        cir.setReturnValue(dev.overgrown.apoli.client.render.DynamicTextures.resolve(
            apoli$slim ? cfg.slim() : cfg.wide(), player));
    }

    @ModifyExpressionValue(method = "renderHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/PlayerSkin;texture()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation apoli$replaceHandSkin(ResourceLocation original, PoseStack pose, MultiBufferSource buffers,
                                                   int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve) {
        CustomModelRenderPower.Config cfg = CustomModelRenderPower.firstReplace(player);
        if (cfg == null || !cfg.showFirstPerson()) {
            return original;
        }
        return dev.overgrown.apoli.client.render.DynamicTextures.resolve(
            apoli$slim ? cfg.slim() : cfg.wide(), player);
    }

    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void apoli$rightArmOverlay(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        apoli$renderArmOverlay(pose, buffers, light, player, this.getModel().rightArm, this.getModel().rightSleeve);
        apoli$renderArmGeometry(pose, buffers, light, player, ModelParts.RIGHT_ARM);
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void apoli$leftArmOverlay(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        apoli$renderArmOverlay(pose, buffers, light, player, this.getModel().leftArm, this.getModel().leftSleeve);
        apoli$renderArmGeometry(pose, buffers, light, player, ModelParts.LEFT_ARM);
    }

    @Unique
    private void apoli$renderArmOverlay(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                        ModelPart arm, ModelPart sleeve) {
        List<ResolvedLayer> layers = CustomModelRenderPower.collectTextureOverlays(player);
        if (layers.isEmpty()) {
            return;
        }
        float alphaScale = GhostArmState.isActive() ? GhostArmState.alpha() : 1.0F;
        PlayerModel<AbstractClientPlayer> model = this.getModel();
        for (ResolvedLayer layer : layers) {
            if (!layer.showFirstPerson() || !apoli$layerAffectsArm(model, layer, arm, sleeve)) {
                continue;
            }
            ResourceLocation texture = layer.texture(apoli$slim);
            int color = FastColor.ARGB32.colorFromFloat(layer.alpha() * alphaScale, layer.red(), layer.green(), layer.blue());
            VertexConsumer consumer = buffers.getBuffer(OverlayRenderTypes.forMode(layer.mode(), texture));
            boolean scaled = layer.scale() != 1.0F;
            if (scaled) {
                pose.pushPose();
                pose.scale(layer.scale(), layer.scale(), layer.scale());
            }
            arm.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);
            sleeve.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);
            if (scaled) {
                pose.popPose();
            }
        }
    }

    @Unique
    private void apoli$renderArmGeometry(PoseStack pose, MultiBufferSource buffers, int light,
                                         AbstractClientPlayer player, String slot) {
        List<GeometryRender> geometry = CustomModelRenderPower.collectGeometry(player);
        if (geometry.isEmpty()) {
            return;
        }
        float alphaScale = GhostArmState.isActive() ? GhostArmState.alpha() : 1.0F;
        PlayerModel<AbstractClientPlayer> model = this.getModel();
        PlayerModel<AbstractClientPlayer> rest = PlayerRestPose.get();
        for (int i = 0; i < geometry.size(); i++) {
            GeometryRender render = geometry.get(i);
            if (!render.showFirstPerson()) {
                continue;
            }
            CustomModel custom = CustomModelManager.get(render.model());
            if (custom == null) {
                continue;
            }
            GeometryRenderer.syncPlayer(custom, model, rest);
            AnimationPlayer.apply(player, render, custom, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            GeometryRenderer.applyVisibility(custom, render.bodyParts());
            GeometryRenderer.drawSlot(render, custom, slot, alphaScale, pose, buffers, light, player);
        }
    }

    @Unique
    private boolean apoli$layerAffectsArm(PlayerModel<AbstractClientPlayer> model, ResolvedLayer layer, ModelPart arm, ModelPart sleeve) {
        if (layer.wholeModel()) {
            return true;
        }
        for (String partName : layer.bodyParts()) {
            List<ModelPart> parts = ModelPartLookup.resolve(model, ModelParts.normalize(partName));
            if (parts.contains(arm) || parts.contains(sleeve)) {
                return true;
            }
        }
        return false;
    }
}
