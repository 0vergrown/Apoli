package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Key(String key, boolean continuous) {
    public static final String PRIMARY_ACTIVE = "key.apoli.primary_active";

    public static final Key DEFAULT_PRIMARY = new Key(PRIMARY_ACTIVE, false);

    private static final Codec<Key> RECORD_CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("key").forGetter(Key::key),
        Codec.BOOL.optionalFieldOf("continuous", false).forGetter(Key::continuous)
    ).apply(i, Key::new));

    public static final Codec<Key> CODEC = Codec.either(Codec.STRING, RECORD_CODEC).xmap(
        either -> either.map(s -> new Key(s, false), k -> k),
        Either::right
    );
}
