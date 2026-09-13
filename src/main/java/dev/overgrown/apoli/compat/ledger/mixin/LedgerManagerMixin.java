package dev.overgrown.apoli.compat.ledger.mixin;

import dev.overgrown.apoli.compat.ledger.ApoliLoggedEntry;
import dev.overgrown.apoli.compat.ledger.LedgerBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "ledger.core.LedgerManager", remap = false)
public abstract class LedgerManagerMixin {

    @Inject(method = "log(Lledger/core/LogEntry;)V", at = @At("HEAD"))
    private void apoli$labelCause(@Coerce Object entry, CallbackInfo ci) {
        if (entry instanceof ApoliLoggedEntry logged) LedgerBridge.stampIndexorEntry(logged);
    }
}
