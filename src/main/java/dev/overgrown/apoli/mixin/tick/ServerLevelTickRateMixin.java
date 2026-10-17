package dev.overgrown.apoli.mixin.tick;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import dev.overgrown.apoli.tick.TickRates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelTickRateMixin {

    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void apoli$gateEntityTick(Entity entity, CallbackInfo ci) {
        if (TickRates.idle()) return;
        if (!TickRates.shouldTickEntity(entity)) ci.cancel();
    }

    @WrapWithCondition(method = "tickPassenger(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private boolean apoli$gatePassengerTick(Entity passenger, Entity vehicle, Entity rider) {
        if (TickRates.idle() || TickRates.shouldTickEntity(passenger)) return true;
        vehicle.positionRider(passenger);
        return false;
    }

    @Inject(method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V", at = @At("HEAD"), cancellable = true)
    private void apoli$gateChunkTick(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (TickRates.idle()) return;
        if (!TickRates.shouldTickChunk((ServerLevel) (Object) this, chunk.getPos().toLong())) ci.cancel();
    }

    @ModifyReturnValue(method = "shouldTickBlocksAt(J)Z", at = @At("RETURN"))
    private boolean apoli$gateBlockTicks(boolean original, long chunkPos) {
        return original && (TickRates.idle() || TickRates.shouldTickChunk((ServerLevel) (Object) this, chunkPos));
    }
}
