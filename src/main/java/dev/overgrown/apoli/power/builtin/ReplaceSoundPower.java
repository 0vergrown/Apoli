package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.data.sound.SoundReplacements;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public abstract class ReplaceSoundPower extends PowerType<ReplaceSoundPower.Config> {
    public record Config(
        SoundReplacements sounds,
        boolean replace,
        Optional<EntityAction> entityAction,
        int priority
    ) {}

    private static final MapCodec<Config> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SoundReplacements.CODEC.fieldOf("sounds").forGetter(Config::sounds),
        Codec.BOOL.optionalFieldOf("replace", true).forGetter(Config::replace),
        EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
        Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
    ).apply(i, Config::new));

    @Override
    public MapCodec<Config> configCodec() {
        return CODEC;
    }

    @Override
    public boolean ticksNonLivingEntities() {
        return true;
    }
}
