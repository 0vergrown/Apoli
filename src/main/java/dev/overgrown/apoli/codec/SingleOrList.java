package dev.overgrown.apoli.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.List;

public final class SingleOrList {

    private SingleOrList() {}

    public static <A> Codec<List<A>> of(Codec<A> element) {
        return Codec.either(element, element.listOf()).xmap(
            either -> either.map(List::of, List::copyOf),
            list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));
    }
}
