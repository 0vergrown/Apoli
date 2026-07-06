package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class ModelParts {
    private ModelParts() {}

    public static final String HEAD = "head";
    public static final String HAT = "hat";
    public static final String BODY = "body";
    public static final String RIGHT_ARM = "rightarm";
    public static final String LEFT_ARM = "leftarm";
    public static final String RIGHT_LEG = "rightleg";
    public static final String LEFT_LEG = "leftleg";

    
    public static final Codec<List<String>> PART_LIST_CODEC = Codec.either(
        Codec.STRING,
        Codec.STRING.listOf()
    ).xmap(
        either -> either.map(List::of, Function.identity()),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
    );

    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "").replace("-", "");
    }
}
