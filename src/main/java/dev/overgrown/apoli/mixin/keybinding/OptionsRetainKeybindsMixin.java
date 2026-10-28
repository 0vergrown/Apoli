package dev.overgrown.apoli.mixin.keybinding;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.overgrown.apoli.client.DynamicKeyMappingManager;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

@Mixin(Options.class)
public abstract class OptionsRetainKeybindsMixin {

    @Shadow @Final private File optionsFile;

    @Inject(method = "save", at = @At("HEAD"))
    private void apoli$readRetainedKeybinds(CallbackInfo ci,
                                            @Share("apoli$retained") LocalRef<Map<String, String>> retained) {
        retained.set(DynamicKeyMappingManager.retainedKeyLines(this.optionsFile, ((Options) (Object) this).keyMappings));
    }

    @Inject(method = "save", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Options;processOptions(Lnet/minecraft/client/Options$FieldAccess;)V",
        shift = At.Shift.AFTER))
    private void apoli$writeRetainedKeybinds(CallbackInfo ci, @Local PrintWriter writer,
                                             @Share("apoli$retained") LocalRef<Map<String, String>> retained) {
        Map<String, String> lines = retained.get();
        if (lines == null) return;
        for (Map.Entry<String, String> line : lines.entrySet()) {
            writer.print(line.getKey());
            writer.print(':');
            writer.println(line.getValue());
        }
    }
}
