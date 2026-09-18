package dev.overgrown.apoli.mixin.item;

import dev.overgrown.apoli.power.builtin.KeepInventoryPower;
import net.minecraft.server.level.ServerPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerKeepInventoryMixin {

    @Inject(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V",
        at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/server/level/ServerPlayer;enchantmentSeed:I"))
    private void apoli$carryKeptInventory(ServerPlayer previous, boolean keepEverything, CallbackInfo ci) {
        if (keepEverything) return;
        if (!KeepInventoryPower.isHeldBy(previous)) return;
        ((ServerPlayer) (Object) this).getInventory().replaceWith(previous.getInventory());
    }
}
