package dev.overgrown.apoli.mixin.tick;

import dev.overgrown.apoli.tick.TickRates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelTickRateMixin {

    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void apoli$gateEntityTick(Entity entity, CallbackInfo ci) {
        if (TickRates.idle()) return;
        if (!TickRates.shouldTickEntity(entity)) ci.cancel();
    }

    @Inject(method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V", at = @At("HEAD"), cancellable = true)
    private void apoli$gateChunkTick(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (TickRates.idle()) return;
        if (!TickRates.shouldTickChunk((ServerLevel) (Object) this, chunk.getPos().toLong())) ci.cancel();
    }

    @Inject(method = "shouldTickBlocksAt(J)Z", at = @At("HEAD"), cancellable = true)
    private void apoli$gateBlockTicks(long chunkPos, CallbackInfoReturnable<Boolean> cir) {
        if (TickRates.idle()) return;
        if (!TickRates.shouldTickChunk((ServerLevel) (Object) this, chunkPos)) cir.setReturnValue(false);
    }
}
