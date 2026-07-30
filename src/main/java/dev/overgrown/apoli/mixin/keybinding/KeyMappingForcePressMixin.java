package dev.overgrown.apoli.mixin.keybinding;

import dev.overgrown.apoli.client.ForcedKeys;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class KeyMappingForcePressMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void apoli$forcedIsDown(CallbackInfoReturnable<Boolean> cir) {
        if (ForcedKeys.isForced((KeyMapping) (Object) this)) cir.setReturnValue(true);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void apoli$forcedConsumeClick(CallbackInfoReturnable<Boolean> cir) {
        if (ForcedKeys.consumeForcedClick((KeyMapping) (Object) this)) cir.setReturnValue(true);
    }
}
