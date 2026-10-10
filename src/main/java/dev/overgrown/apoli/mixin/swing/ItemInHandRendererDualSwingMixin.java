package dev.overgrown.apoli.mixin.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.access.DualSwingHolder;
import dev.overgrown.apoli.client.render.ArmEdits;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemInHandRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class ItemInHandRendererDualSwingMixin {

    @ModifyArg(
        method = "renderHandsWithItems",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
        index = 4)
    private float apoli$swingProgress(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                      float swingProgress, ItemStack stack, float equippedProgress, PoseStack pose,
                                      MultiBufferSource buffers, int light) {
        if (ArmEdits.held(ArmEdits.of(player, true), ArmEdits.armFor(player, hand))) return 0.0F;
        if (swingProgress > 0.0F || !DualSwingHolder.of(player)) return swingProgress;
        return player.getAttackAnim(partialTick);
    }

    @ModifyArg(
        method = "renderHandsWithItems",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
        index = 6)
    private float apoli$equippedProgress(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                         float swingProgress, ItemStack stack, float equippedProgress, PoseStack pose,
                                         MultiBufferSource buffers, int light) {
        return ArmEdits.held(ArmEdits.of(player, true), ArmEdits.armFor(player, hand)) ? 0.0F : equippedProgress;
    }
}
