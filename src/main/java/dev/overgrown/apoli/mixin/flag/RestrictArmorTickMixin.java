package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.RestrictArmorPower;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class RestrictArmorTickMixin {

    private static final int SWEEP_INTERVAL = 20;

    @Inject(method = "tick", at = @At("RETURN"))
    private void apoli$sweepRestrictedArmor(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.tickCount % SWEEP_INTERVAL != 0) return;
        if (self.level().isClientSide()) return;
        if (!PowerLookup.hasActive(self, ApoliIds.RESTRICT_ARMOR)) return;
        RestrictArmorPower.unequipRestricted(self);
    }
}
