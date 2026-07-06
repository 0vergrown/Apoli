package dev.overgrown.apoli.power.builtin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface EntityGameEventListenerAccess {
    @Nullable
    Map<ResourceLocation, GameEventListenerPower.State> apoli$getGameEventStates();

    void apoli$setGameEventStates(@Nullable Map<ResourceLocation, GameEventListenerPower.State> map);

    @Nullable
    static Map<ResourceLocation, GameEventListenerPower.State> getMap(Entity entity) {
        return entity instanceof EntityGameEventListenerAccess access ? access.apoli$getGameEventStates() : null;
    }

    static void setMap(Entity entity, @Nullable Map<ResourceLocation, GameEventListenerPower.State> map) {
        if (entity instanceof EntityGameEventListenerAccess access) access.apoli$setGameEventStates(map);
    }
}
