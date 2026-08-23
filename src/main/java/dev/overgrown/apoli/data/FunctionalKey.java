package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;

import java.util.Optional;

public record FunctionalKey(Key key, Optional<EntityAction> action) {

    private static final Codec<FunctionalKey> RECORD_CODEC = RecordCodecBuilder.create(i -> i.group(
        Key.MAP_CODEC.forGetter(FunctionalKey::key),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("action", EntityAction.CODEC).forGetter(FunctionalKey::action)
    ).apply(i, FunctionalKey::new));

    public static final Codec<FunctionalKey> CODEC = Codec.either(Codec.STRING, RECORD_CODEC).xmap(
        either -> either.map(s -> new FunctionalKey(new Key(s, false), Optional.empty()), fk -> fk),
        fk -> fk.action.isEmpty() && !fk.key.continuous() ? Either.left(fk.key.key()) : Either.right(fk)
    );
}
