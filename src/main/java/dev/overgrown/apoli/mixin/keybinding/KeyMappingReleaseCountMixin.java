package dev.overgrown.apoli.mixin.keybinding;

import dev.overgrown.apoli.access.KeyReleaseCount;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyMappingReleaseCountMixin implements KeyReleaseCount {
    @Shadow
    private boolean isDown;

    @Unique
    private int apoli$releases;

    @Inject(method = "setDown", at = @At("HEAD"))
    private void apoli$countRelease(boolean down, CallbackInfo ci) {
        if (this.isDown && !down) this.apoli$releases++;
    }

    @Override
    public int apoli$releases() {
        return this.apoli$releases;
    }
}
