package dev.overgrown.apoli.mixin.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ItemPickupHandler;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobPickupMixin {

    @Unique
    private ItemPickupHandler.Pending apoli$pickup;

    @ModifyExpressionValue(method = "aiStep()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Mob;wantsToPickUp(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean apoli$beforeMobPickup(boolean wants, @Local ItemEntity item) {
        this.apoli$pickup = null;
        if (!wants) return false;
        Mob self = (Mob) (Object) this;
        if (!ItemPickupHandler.watches(self)) return true;
        if (ItemPickupHandler.prevents(item, self)) return false;
        this.apoli$pickup = ItemPickupHandler.before(item, self, true);
        return true;
    }

    @Inject(method = "aiStep()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Mob;pickUpItem(Lnet/minecraft/world/entity/item/ItemEntity;)V",
        shift = At.Shift.AFTER))
    private void apoli$afterMobPickup(CallbackInfo ci) {
        ItemPickupHandler.Pending pending = this.apoli$pickup;
        if (pending == null) return;
        this.apoli$pickup = null;
        if (pending.taken()) ItemPickupHandler.after(pending, (Mob) (Object) this);
    }
}
