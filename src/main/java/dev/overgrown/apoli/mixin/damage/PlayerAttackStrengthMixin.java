package dev.overgrown.apoli.mixin.damage;

import dev.overgrown.apoli.data.AttackStrengthContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackStrengthMixin {

    @Unique
    private long apoli$previousAttackStrength = AttackStrengthContext.NONE;

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void apoli$captureAttackStrength(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        apoli$previousAttackStrength = AttackStrengthContext.set(self.getId(), self.getAttackStrengthScale(0.5f));
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void apoli$releaseAttackStrength(Entity target, CallbackInfo ci) {
        AttackStrengthContext.restore(apoli$previousAttackStrength);
        apoli$previousAttackStrength = AttackStrengthContext.NONE;
    }
}
