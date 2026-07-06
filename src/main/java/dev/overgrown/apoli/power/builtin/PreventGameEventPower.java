package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.Optional;

public final class PreventGameEventPower extends PowerType<PreventGameEventPower.Config> {
    public record Config(
        Optional<ResourceLocation> event,
        Optional<List<ResourceLocation>> events,
        Optional<ResourceLocation> tag,
        Optional<EntityAction> entityAction
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("event").forGetter(Config::event),
            ResourceLocation.CODEC.listOf().optionalFieldOf("events").forGetter(Config::events),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(Config::tag),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction)
        ).apply(i, Config::new));
    }

    public static boolean matches(Config cfg, GameEvent gameEvent) {
        if (cfg.event().isPresent()) {
            GameEvent configured = BuiltInRegistries.GAME_EVENT.get(cfg.event().get());
            if (gameEvent == configured) return true;
        }
        if (cfg.events().isPresent()) {
            for (ResourceLocation id : cfg.events().get()) {
                if (BuiltInRegistries.GAME_EVENT.get(id) == gameEvent) return true;
            }
        }
        if (cfg.tag().isPresent()) {
            TagKey<GameEvent> tag = TagKey.create(Registries.GAME_EVENT, cfg.tag().get());
            if (gameEvent.is(tag)) return true;
        }
        return false;
    }

    public static void executeAction(Config cfg, ServerLevel level, net.minecraft.world.entity.Entity entity) {
        cfg.entityAction().ifPresent(a -> a.run(EntityCtx.of(entity, level)));
    }
}
