package dev.overgrown.apoli.mixin.block;

import dev.overgrown.apoli.power.builtin.BlockPlaceHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {

    private static final String PLACE =
        "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;";

    @Inject(method = PLACE, at = @At("HEAD"), cancellable = true)
    private void apoli$preventBlockPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null) return;
        if (BlockPlaceHandler.isPrevented(player, context.getLevel(), context.getHand(),
            context.getItemInHand(), context.getClickedPos(), apoli$onPos(context), context.getClickedFace())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = PLACE, at = @At("RETURN"))
    private void apoli$actionOnBlockPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        Player player = context.getPlayer();
        if (player == null) return;
        Level level = context.getLevel();
        InteractionHand hand = context.getHand();
        BlockPos toPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack placed = context.getItemInHand();
        if (placed.isEmpty()) placed = new ItemStack(((BlockItem) (Object) this));
        BlockPlaceHandler.fireAfterPlace(player, level, hand, placed, toPos, apoli$onPos(context), face);
    }

    private static BlockPos apoli$onPos(BlockPlaceContext context) {
        return ((UseOnContextAccessor) context).apoli$getHitResult().getBlockPos();
    }
}
