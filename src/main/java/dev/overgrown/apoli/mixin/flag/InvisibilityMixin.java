package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.InvisibilityPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class InvisibilityMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void apoli$invisibility(Player observer, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LivingEntity self)) return;
        boolean[] hide = new boolean[]{false};
        PowerLookup.forEach(self, Apoli.id("invisibility"), InvisibilityPower.Config.class, cfg -> {
            if (hide[0]) return;
            if (cfg.bientityCondition().isPresent()) {
                if (observer == null) return;
                if (!cfg.bientityCondition().get().test(new BiEntityCtx(observer, self, self.level()))) return;
            }
            hide[0] = true;
        });
        if (hide[0]) cir.setReturnValue(true);
    }
}
