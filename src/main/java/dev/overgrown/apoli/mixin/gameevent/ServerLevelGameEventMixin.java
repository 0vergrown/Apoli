package dev.overgrown.apoli.mixin.gameevent;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PreventGameEventPower;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelGameEventMixin {

    @Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V", at = @At("HEAD"), cancellable = true)
    private void apoli$preventGameEvent(Holder<GameEvent> holder, Vec3 vec3, GameEvent.Context context, CallbackInfo ci) {
        Entity source = context.sourceEntity();
        if (source == null) return;
        ServerLevel self = (ServerLevel) (Object) this;
        PowerLookup.forEach(source, Apoli.id("prevent_game_event"), PreventGameEventPower.Config.class, cfg -> {
            if (PreventGameEventPower.matches(cfg, holder)) {
                PreventGameEventPower.executeAction(cfg, self, source);
                ci.cancel();
            }
        });
    }
}
