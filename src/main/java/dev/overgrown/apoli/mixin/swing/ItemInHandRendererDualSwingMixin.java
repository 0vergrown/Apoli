package dev.overgrown.apoli.mixin.swing;

import dev.overgrown.apoli.access.DualSwingHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ItemInHandRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class ItemInHandRendererDualSwingMixin {

    @ModifyArgs(
        method = "renderHandsWithItems",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void apoli$dualSwingProgress(Args args) {
        float swingProgress = args.get(4);
        if (swingProgress > 0.0F) return;
        AbstractClientPlayer player = args.get(0);
        if (!DualSwingHolder.of(player)) return;
        args.set(4, player.getAttackAnim(args.<Float>get(1)));
    }
}
