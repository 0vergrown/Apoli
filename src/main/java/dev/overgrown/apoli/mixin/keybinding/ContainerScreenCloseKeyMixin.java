package dev.overgrown.apoli.mixin.keybinding;

import dev.overgrown.apoli.client.ScreenCloseKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenCloseKeyMixin {

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void apoli$closeOnOpeningKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        Screen self = (Screen) (Object) this;
        if (!ScreenCloseKeys.closes(self, keyCode, scanCode)) return;
        if (this.hoveredSlot != null && ScreenCloseKeys.isSlotKey(Minecraft.getInstance().options, keyCode, scanCode)) return;
        self.onClose();
        cir.setReturnValue(true);
    }
}
