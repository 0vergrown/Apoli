package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyHarvestHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerHarvestMixin {
    @Inject(method = "hasCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void apoli$modifyHarvest(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        Boolean forced = ModifyHarvestHandler.modify((Player) (Object) this, state);
        if (forced != null) {
            cir.setReturnValue(forced);
        }
    }
}
