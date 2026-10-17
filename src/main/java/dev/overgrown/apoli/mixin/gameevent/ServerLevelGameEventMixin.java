package dev.overgrown.apoli.mixin.gameevent;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import dev.overgrown.apoli.power.builtin.PreventGameEventPower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public class ServerLevelGameEventMixin {

    @WrapWithCondition(method = "gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gameevent/GameEventDispatcher;post(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V"))
    private boolean apoli$preventGameEvent(GameEventDispatcher dispatcher, GameEvent event, Vec3 pos,
                                           GameEvent.Context context) {
        Entity source = context.sourceEntity();
        return source == null || !PreventGameEventPower.prevent((ServerLevel) (Object) this, source, event);
    }
}
