package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ClimbingPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFlagMixin {
    @Shadow private Optional<BlockPos> lastClimbablePos;

    @ModifyReturnValue(method = "onClimbable", at = @At("RETURN"))
    private boolean apoli$climbing(boolean original) {
        if (original) return true;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isSpectator() || !ClimbingPower.mayHold(self)) return false;
        if (ClimbingPower.state(self, false) == ClimbingPower.NONE) return false;
        this.lastClimbablePos = Optional.of(self.blockPosition());
        return true;
    }

    @ModifyReturnValue(method = "isSuppressingSlidingDownLadder", at = @At("RETURN"))
    private boolean apoli$climbingHold(boolean original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!ClimbingPower.mayHold(self)) return original;
        int state = ClimbingPower.state(self, true);
        if (state == ClimbingPower.NONE) return original;
        return state == ClimbingPower.HOLDS;
    }
}
