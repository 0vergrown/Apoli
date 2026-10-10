package dev.overgrown.apoli.mixin.firstperson;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.access.PartVisibilityUndo;
import dev.overgrown.apoli.client.render.FirstPersonHeldItem;
import dev.overgrown.apoli.client.render.HandRenderPass;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class PlayerRendererHandPassMixin {

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void apoli$beginHandPass(PoseStack poseStack, MultiBufferSource buffers, int light,
                                     AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                     CallbackInfo ci) {
        HandRenderPass.begin();
    }

    @WrapOperation(method = "renderHand", at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/model/geom/ModelPart;xRot:F", opcode = Opcodes.PUTFIELD))
    private void apoli$keepEditedPitch(ModelPart part, float value, Operation<Void> original) {
        if (!HandRenderPass.edited()) original.call(part, value);
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void apoli$endHandPass(PoseStack poseStack, MultiBufferSource buffers, int light,
                                   AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                   CallbackInfo ci) {
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        if (model instanceof PartVisibilityUndo undo) {
            undo.apoli$undoPartVisibility();
        }
        HandRenderPass.end();
        FirstPersonHeldItem.render(model, arm, player, poseStack, buffers, light);
    }
}
