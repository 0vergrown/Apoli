package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.InvisibilityPower;
import dev.overgrown.apoli.power.ApoliIds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class InvisibilityMixin {
    @ModifyReturnValue(method = "isInvisibleTo", at = @At("RETURN"))
    private boolean apoli$invisibility(boolean original, Player observer) {
        if (original || !InvisibilityPower.inUse() || !((Object) this instanceof LivingEntity self)) return original;
        PowerContainer container = PowerContainer.of(self);
        if (container == null || container.isEmpty() || container.powersOfType(ApoliIds.INVISIBILITY).isEmpty()) return false;
        boolean[] hide = new boolean[]{false};
        PowerLookup.forEach(self, ApoliIds.INVISIBILITY, InvisibilityPower.Config.class, cfg -> {
            if (hide[0]) return;
            if (cfg.bientityCondition().isPresent()) {
                if (observer == null) return;
                if (!cfg.bientityCondition().get().test(new BiEntityCtx(observer, self, self.level()))) return;
            }
            hide[0] = true;
        });
        return hide[0];
    }
}
