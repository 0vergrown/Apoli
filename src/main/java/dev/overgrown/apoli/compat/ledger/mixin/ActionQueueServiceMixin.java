package dev.overgrown.apoli.compat.ledger.mixin;

import dev.overgrown.apoli.compat.ledger.LedgerBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.quiltservertools.ledger.database.ActionQueueService", remap = false)
public abstract class ActionQueueServiceMixin {

    @Inject(method = "addToQueue(Lcom/github/quiltservertools/ledger/actions/ActionType;)Z", at = @At("HEAD"))
    private void apoli$labelCause(@Coerce Object action, CallbackInfoReturnable<Boolean> cir) {
        LedgerBridge.stampLedgerAction(action);
    }
}
