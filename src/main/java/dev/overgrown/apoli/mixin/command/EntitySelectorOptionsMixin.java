package dev.overgrown.apoli.mixin.command;

import dev.overgrown.apoli.command.ApoliSelectorOptions;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntitySelectorOptions.class)
public class EntitySelectorOptionsMixin {

    @Inject(method = "bootStrap", at = @At("TAIL"))
    private static void apoli$registerSelectorOptions(CallbackInfo ci) {
        ApoliSelectorOptions.bootstrap();
    }
}
