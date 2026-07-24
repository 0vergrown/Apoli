package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.gameevent.GameEvent;

public final class EmitGameEventAction implements ActionType<EntityCtx, EmitGameEventAction.Cfg> {
    public record Cfg(ResourceLocation event) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("event").forGetter(Cfg::event)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        ResourceKey<GameEvent> key = ResourceKey.create(Registries.GAME_EVENT, cfg.event);
        Holder<GameEvent> holder = BuiltInRegistries.GAME_EVENT.getHolder(key).orElse(null);
        if (holder == null) return;
        ctx.raw().gameEvent(holder);
    }
}
