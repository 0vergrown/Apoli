package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.PreventItemUseHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void apoli$preventItemUse(Level level, Player player, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (PreventItemUseHandler.isBlocked(player, self, level)) {
            cir.setReturnValue(InteractionResultHolder.fail(self));
        }
    }
}
