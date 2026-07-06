package dev.overgrown.apoli.mixin.gameevent;

import dev.overgrown.apoli.power.builtin.EntityGameEventListenerAccess;
import dev.overgrown.apoli.power.builtin.GameEventListenerPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(Entity.class)
public abstract class EntityGameEventListenerMixin implements EntityGameEventListenerAccess {

    @Unique
    @Nullable
    private Map<ResourceLocation, GameEventListenerPower.State> apoli$gameEventStates;

    @Override
    @Nullable
    public Map<ResourceLocation, GameEventListenerPower.State> apoli$getGameEventStates() {
        return apoli$gameEventStates;
    }

    @Override
    public void apoli$setGameEventStates(@Nullable Map<ResourceLocation, GameEventListenerPower.State> map) {
        this.apoli$gameEventStates = map;
    }

    @Inject(method = "updateDynamicGameEventListener", at = @At("HEAD"))
    private void apoli$updateGameEventListeners(BiConsumer<DynamicGameEventListener<?>, ServerLevel> callback, CallbackInfo ci) {
        if (apoli$gameEventStates == null || apoli$gameEventStates.isEmpty()) return;
        Entity self = (Entity) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;
        for (GameEventListenerPower.State state : apoli$gameEventStates.values()) {
            callback.accept(state.getEventHandler(), serverLevel);
        }
    }
}
