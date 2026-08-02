package dev.overgrown.apoli.mixin.firstperson;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.power.builtin.ShowBothArmsPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class ItemInHandRendererBothArmsMixin {

    @Shadow
    protected abstract void renderPlayerArm(PoseStack pose, MultiBufferSource buffers, int light,
                                            float equippedProgress, float swingProgress, HumanoidArm arm);

    @Inject(method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0))
    private void apoli$showBothArms(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                    float swingProgress, ItemStack stack, float equippedProgress,
                                    PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (player.isInvisible()) return;
        boolean main = hand == InteractionHand.MAIN_HAND;
        if (main ? stack.isEmpty() || stack.is(Items.FILLED_MAP) : !stack.isEmpty()) return;

        int mask = ShowBothArmsPower.armMask(player);
        if ((mask & (main ? ShowBothArmsPower.MAIN : ShowBothArmsPower.OFF)) == 0) return;

        HumanoidArm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        pose.pushPose();
        this.renderPlayerArm(pose, buffers, light, equippedProgress, swingProgress, arm);
        pose.popPose();
    }
}
