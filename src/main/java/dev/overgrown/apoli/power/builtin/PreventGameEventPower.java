package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

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
}
