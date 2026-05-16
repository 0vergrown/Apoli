package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Key(String key, boolean continuous) {
    public static final String PRIMARY_ACTIVE = "key.apoli.primary_active";

    public static final Key DEFAULT_PRIMARY = new Key(PRIMARY_ACTIVE, false);

    public static final Codec<Key> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("key").forGetter(Key::key),
        Codec.BOOL.optionalFieldOf("continuous", false).forGetter(Key::continuous)
    ).apply(i, Key::new));
}