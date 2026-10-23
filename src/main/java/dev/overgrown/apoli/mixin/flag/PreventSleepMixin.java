package dev.overgrown.apoli.mixin.flag;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import dev.overgrown.apoli.power.builtin.PreventSleepHandler;
import dev.overgrown.apoli.power.builtin.SleepDecisionHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PreventSleepMixin implements SleepDecisionHolder {
    @Unique
    private PreventSleepHandler.Decision apoli$sleepDecision;

    @Inject(method = "startSleepInBed(Lnet/minecraft/core/BlockPos;)Lcom/mojang/datafixers/util/Either;", at = @At("HEAD"))
    private void apoli$decideSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.apoli$sleepDecision = PreventSleepHandler.decide((ServerPlayer) (Object) this, pos);
    }

    @Inject(method = "startSleepInBed(Lnet/minecraft/core/BlockPos;)Lcom/mojang/datafixers/util/Either;", at = @At("RETURN"))
    private void apoli$forgetSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.apoli$sleepDecision = null;
    }

    @Inject(method = "setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V",
        at = @At("HEAD"), cancellable = true)
    private void apoli$keepSpawn(ResourceKey<Level> dimension, @Nullable BlockPos pos, float angle, boolean forced,
                                 boolean sendMessage, CallbackInfo ci) {
        PreventSleepHandler.Decision decision = this.apoli$sleepDecision;
        if (decision != null && !decision.setsSpawn() && decision.pos().equals(pos)) ci.cancel();
    }

    @Override
    public PreventSleepHandler.Decision apoli$takeSleepDecision() {
        PreventSleepHandler.Decision decision = this.apoli$sleepDecision;
        this.apoli$sleepDecision = null;
        return decision;
    }
}
