package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ClimbingPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFlagMixin {
    @Shadow private Optional<BlockPos> lastClimbablePos;

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void apoli$climbing(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isSpectator() || !PowerLookup.hasActive(self, Apoli.id("climbing"))) return;
        this.lastClimbablePos = Optional.of(self.blockPosition());
        cir.setReturnValue(true);
    }

    @Inject(method = "isSuppressingSlidingDownLadder", at = @At("RETURN"), cancellable = true)
    private void apoli$climbingHold(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        EntityCtx ctx = new EntityCtx(self, self.level());
        boolean[] hasPower = {false};
        boolean[] canHold = {false};
        PowerLookup.forEach(self, Apoli.id("climbing"), ClimbingPower.Config.class, cfg -> {
            hasPower[0] = true;
            if (canHold[0] || !cfg.allowHolding()) return;
            boolean held = cfg.holdCondition().map(c -> c.test(ctx)).orElseGet(self::isShiftKeyDown);
            if (held) canHold[0] = true;
        });
        if (hasPower[0]) cir.setReturnValue(canHold[0]);
    }
}
