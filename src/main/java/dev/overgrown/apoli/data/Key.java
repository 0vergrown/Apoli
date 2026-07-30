package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;

import java.util.Map;

public record Key(String key, boolean continuous) {
    public static final String PRIMARY_ACTIVE = "key.apoli.primary_active";

    public static final Key DEFAULT_PRIMARY = new Key(PRIMARY_ACTIVE, false);

    public static final MapCodec<Key> MAP_CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<Key>mapCodec(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(Key::key),
            Codec.BOOL.optionalFieldOf("continuous", false).forGetter(Key::continuous)
        ).apply(i, Key::new)),
        Map.of("continous", "continuous"));

    public static final Codec<Key> CODEC = Codec.either(Codec.STRING, MAP_CODEC.codec()).xmap(
        either -> either.map(s -> new Key(s, false), k -> k),
        Either::right
    );
}
