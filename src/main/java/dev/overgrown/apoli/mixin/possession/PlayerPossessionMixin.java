package dev.overgrown.apoli.mixin.possession;

import dev.overgrown.apoli.entity.possession.Possession;
import dev.overgrown.apoli.entity.possession.PossessionManager;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerPossessionMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void apoli$hostTakesTheHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        Entity host = Possession.hostOf(self);
        if (host == null || self.level().isClientSide() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (source.getEntity() != host) host.hurt(source, amount);
        cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void apoli$hostAttacks(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        Entity host = Possession.hostOf(self);
        if (host == null) return;
        if (target == host) {
            ci.cancel();
            return;
        }
        if (self.level().isClientSide() || !(host instanceof Mob mob) || !PossessionManager.hostAttacks(self)) return;
        boolean charged = self.getAttackStrengthScale(0.5f) > 0.9f;
        self.resetAttackStrengthTicker();
        if (charged && target.isAttackable() && !target.skipAttackInteraction(self)) {
            mob.doHurtTarget(target);
        }
        mob.swing(InteractionHand.MAIN_HAND, true);
        ci.cancel();
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void apoli$leaveOwnHostAlone(Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (target != null && target == Possession.hostOf((Player) (Object) this)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
