package dev.overgrown.apoli.mixin.keybinding;

import dev.overgrown.apoli.client.DynamicKeyMappingManager;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageKeybindNameMixin {

    @Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
        at = @At("RETURN"), cancellable = true)
    private void apoli$dynamicKeybindName(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (!DynamicKeyMappingManager.hasNameHints()) return;
        if (cir.getReturnValue() != fallback) return;
        String hint = DynamicKeyMappingManager.nameHint(key);
        if (hint != null) cir.setReturnValue(hint);
    }
}
