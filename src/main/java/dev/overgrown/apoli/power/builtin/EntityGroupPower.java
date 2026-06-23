package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;

public final class EntityGroupPower extends PowerType<EntityGroupPower.Cfg> {
    public record Cfg(String group) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "default").forGetter(Cfg::group)
        ).apply(i, Cfg::new));
    }
}
