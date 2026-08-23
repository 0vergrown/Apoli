package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.IdOrTag;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.Optional;

public final class PreventGameEventPower extends PowerType<PreventGameEventPower.Config> {
    public record Config(
        Optional<IdOrTag<GameEvent>> event,
        Optional<List<IdOrTag<GameEvent>>> events,
        Optional<ResourceLocation> tag,
        Optional<EntityAction> entityAction
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdOrTag.codec(Registries.GAME_EVENT).optionalFieldOf("event").forGetter(Config::event),
            IdOrTag.codec(Registries.GAME_EVENT).listOf().optionalFieldOf("events").forGetter(Config::events),
            IdCodecs.TAG.optionalFieldOf("tag").forGetter(Config::tag),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction)
        ).apply(i, Config::new));
    }

    public static boolean matches(Config cfg, GameEvent gameEvent) {
        if (cfg.event().isPresent() && cfg.event().get().matches(gameEvent.builtInRegistryHolder())) return true;
        if (cfg.events().isPresent()) {
            List<IdOrTag<GameEvent>> events = cfg.events().get();
            for (int i = 0; i < events.size(); i++) {
                if (events.get(i).matches(gameEvent.builtInRegistryHolder())) return true;
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
