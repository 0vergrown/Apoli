package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.builtin.RestrictArmorPower;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Equipable.class)
public interface EquipableRestrictArmorMixin {

    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void apoli$preventRestrictedEquip(Item item, Level level, Player player, InteractionHand hand,
                                              CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (RestrictArmorPower.restricts(player, ((Equipable) (Object) this).getEquipmentSlot(), stack)) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
        }
    }
}
