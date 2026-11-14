package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ActionOnAdvancementHandler;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z", at = @At(value = "INVOKE", target = "net/minecraft/server/PlayerAdvancements.markForVisibilityUpdate (Lnet/minecraft/advancements/AdvancementHolder;)V"))
    private void apoli$onAdvancementEvent(AdvancementHolder advancementHolder, String string, CallbackInfoReturnable<Boolean> cir) {
        ActionOnAdvancementHandler.run(player, advancementHolder);
    }

}
