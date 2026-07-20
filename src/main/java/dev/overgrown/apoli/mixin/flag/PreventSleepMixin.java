package dev.overgrown.apoli.mixin.flag;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PreventSleepPower;
import dev.overgrown.apoli.power.ApoliIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PreventSleepMixin {
    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void apoli$preventSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        Player self = (Player) (Object) this;
        Level level = self.level();
        BlockState state = level.getBlockState(pos);
        BlockCtx ctx = new BlockCtx(pos.immutable(), state, level);
        PreventSleepPower.Config[] best = new PreventSleepPower.Config[]{null};
        PowerLookup.forEach(self, ApoliIds.PREVENT_SLEEP, PreventSleepPower.Config.class, cfg -> {
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
            if (best[0] == null || cfg.priority() > best[0].priority()) {
                best[0] = cfg;
            }
        });
        if (best[0] == null) return;
        if (self instanceof ServerPlayer sp) {
            sp.displayClientMessage(best[0].message(), true);
        }
        cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
    }
}
