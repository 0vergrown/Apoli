package dev.overgrown.apoli.mixin.redstone;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.block.RedstonePulses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SignalGetter.class)
public interface SignalGetterPulseMixin {

    @ModifyReturnValue(method = "getSignal", at = @At("RETURN"))
    private int apoli$pulseSignal(int original, BlockPos pos, Direction direction) {
        return RedstonePulses.signal((SignalGetter) this, pos, direction, original, false);
    }

    @ModifyReturnValue(method = "getDirectSignal", at = @At("RETURN"))
    private int apoli$pulseDirectSignal(int original, BlockPos pos, Direction direction) {
        return RedstonePulses.signal((SignalGetter) this, pos, direction, original, true);
    }
}
