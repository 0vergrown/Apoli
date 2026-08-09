package dev.overgrown.apoli.mixin.firstperson;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.GhostArmState;
import dev.overgrown.apoli.power.builtin.ShowBothArmsPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class ItemInHandRendererBothArmsMixin {

    @Shadow
    protected abstract void renderPlayerArm(PoseStack pose, MultiBufferSource buffers, int light,
                                            float equippedProgress, float swingProgress, HumanoidArm arm);

    @Unique
    private ShowBothArmsPower.Arms apoli$arms;

    @Inject(method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0))
    private void apoli$showBothArms(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                    float swingProgress, ItemStack stack, float equippedProgress,
                                    PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (player.isInvisible() || stack.is(Items.FILLED_MAP)) return;
        if (this.apoli$arms == null) this.apoli$arms = new ShowBothArmsPower.Arms();
        if (!ShowBothArmsPower.resolve(player, this.apoli$arms)) return;

        boolean main = hand == InteractionHand.MAIN_HAND;
        boolean drawnByVanilla = main && stack.isEmpty();
        boolean extraArm = !drawnByVanilla
            && (main ? !stack.isEmpty() : stack.isEmpty())
            && (this.apoli$arms.mask & (main ? ShowBothArmsPower.MAIN : ShowBothArmsPower.OFF)) != 0;
        if (!drawnByVanilla && !extraArm) return;

        HumanoidArm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        apoli$renderGhostArms(pose, buffers, light, equippedProgress, swingProgress, arm);

        if (extraArm) {
            pose.pushPose();
            this.renderPlayerArm(pose, buffers, light, equippedProgress, swingProgress, arm);
            pose.popPose();
        }
    }

    @Unique
    private void apoli$renderGhostArms(PoseStack pose, MultiBufferSource buffers, int light,
                                       float equippedProgress, float swingProgress, HumanoidArm arm) {
        int ghosts = this.apoli$arms.ghosts;
        if (ghosts <= 0 || swingProgress <= 0.0F) return;
        float spacing = this.apoli$arms.spacing;
        if (spacing <= 0.0F) return;

        GhostArmState.begin(this.apoli$arms.alpha);
        try {
            for (int i = ghosts; i >= 1; i--) {
                float lag = spacing * i;
                if (lag >= 1.0F) continue;
                float progress = swingProgress - lag;
                if (progress <= 0.0F) progress += 1.0F;
                pose.pushPose();
                this.renderPlayerArm(pose, buffers, light, equippedProgress, progress, arm);
                pose.popPose();
            }
        } finally {
            GhostArmState.end();
        }
    }
}
