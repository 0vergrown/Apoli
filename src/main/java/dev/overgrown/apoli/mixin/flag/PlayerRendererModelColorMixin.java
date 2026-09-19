package dev.overgrown.apoli.mixin.flag;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.ModelColorState;
import dev.overgrown.apoli.client.render.PartColorMap;
import dev.overgrown.apoli.power.builtin.ModelColorPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(PlayerRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerRendererModelColorMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    protected PlayerRendererModelColorMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadow) {
        super(ctx, model, shadow);
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"))
    private void apoli$rightHandColorSetup(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        apoli$setupHandColor(player, this.getModel().rightArm, this.getModel().rightSleeve);
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"))
    private void apoli$leftHandColorSetup(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        apoli$setupHandColor(player, this.getModel().leftArm, this.getModel().leftSleeve);
    }

    @Inject(method = {"renderRightHand", "renderLeftHand"}, at = @At("RETURN"))
    private void apoli$handColorClear(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        ModelColorState.clear();
    }

    @Unique
    private void apoli$setupHandColor(AbstractClientPlayer player, ModelPart arm, ModelPart sleeve) {
        ModelColorState.set(PartColorMap.buildHand(player, this.getModel(), arm, sleeve, ModelColorPower.colorFor(player)));
    }
}
