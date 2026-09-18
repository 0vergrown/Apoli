package dev.overgrown.apoli.mixin.item;

import dev.overgrown.apoli.power.builtin.KeepInventoryPower;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Player.class)
public abstract class PlayerKeepInventoryMixin {

    @Unique
    private List<KeepInventoryPower.Kept> apoli$keptOnDeath;

    @Inject(method = "dropEquipment()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;dropAll()V"))
    private void apoli$holdKeptItems(CallbackInfo ci) {
        this.apoli$keptOnDeath = KeepInventoryPower.takeKept((Player) (Object) this);
    }

    @Inject(method = "dropEquipment()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;dropAll()V",
            shift = At.Shift.AFTER))
    private void apoli$returnKeptItems(CallbackInfo ci) {
        List<KeepInventoryPower.Kept> kept = this.apoli$keptOnDeath;
        this.apoli$keptOnDeath = null;
        if (kept == null || kept.isEmpty()) return;
        KeepInventoryPower.putBack((Player) (Object) this, kept);
    }
}
