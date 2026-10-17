package dev.overgrown.apoli.mixin.keybinding;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.BlockedKeys;
import dev.overgrown.apoli.client.ForcedKeys;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class KeyMappingForcePressMixin {
    @ModifyReturnValue(method = "isDown", at = @At("RETURN"))
    private boolean apoli$forcedIsDown(boolean original) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (BlockedKeys.blocks(self)) return false;
        return original || ForcedKeys.isForced(self);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void apoli$forcedConsumeClick(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (BlockedKeys.blocks(self)) {
            cir.setReturnValue(false);
            return;
        }
        if (ForcedKeys.consumeForcedClick(self)) cir.setReturnValue(true);
    }
}
