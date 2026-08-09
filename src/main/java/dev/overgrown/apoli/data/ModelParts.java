package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static final Map<String, String> SLOTS = Map.ofEntries(
        Map.entry(HEAD, HEAD),
        Map.entry(HAT, HAT),
        Map.entry("headwear", HAT),
        Map.entry("hatlayer", HAT),
        Map.entry("headlayer", HAT),
        Map.entry(BODY, BODY),
        Map.entry("torso", BODY),
        Map.entry("waist", BODY),
        Map.entry("jacket", BODY),
        Map.entry("bodylayer", BODY),
        Map.entry(RIGHT_ARM, RIGHT_ARM),
        Map.entry("armright", RIGHT_ARM),
        Map.entry("rightsleeve", RIGHT_ARM),
        Map.entry("rightarmlayer", RIGHT_ARM),
        Map.entry(LEFT_ARM, LEFT_ARM),
        Map.entry("armleft", LEFT_ARM),
        Map.entry("leftsleeve", LEFT_ARM),
        Map.entry("leftarmlayer", LEFT_ARM),
        Map.entry(RIGHT_LEG, RIGHT_LEG),
        Map.entry("legright", RIGHT_LEG),
        Map.entry("rightpants", RIGHT_LEG),
        Map.entry("rightleglayer", RIGHT_LEG),
        Map.entry(LEFT_LEG, LEFT_LEG),
        Map.entry("legleft", LEFT_LEG),
        Map.entry("leftpants", LEFT_LEG),
        Map.entry("leftleglayer", LEFT_LEG)
    );

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

    @Nullable
    public static String slot(String normalized) {
        return SLOTS.get(normalized);
    }
}
