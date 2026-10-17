package dev.overgrown.apoli.mixin.keybinding;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.DynamicKeyMappingManager;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageKeybindNameMixin {

    @ModifyReturnValue(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"))
    private String apoli$dynamicKeybindName(String original, String key, String fallback) {
        if (original != fallback || !DynamicKeyMappingManager.hasNameHints()) return original;
        String hint = DynamicKeyMappingManager.nameHint(key);
        return hint != null ? hint : original;
    }
}
