package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.codec.SingleOrList;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum Perspective {
    FIRST_PERSON,
    THIRD_PERSON;

    public static final List<Perspective> BOTH = List.of(FIRST_PERSON, THIRD_PERSON);
    public static final List<Perspective> THIRD_ONLY = List.of(THIRD_PERSON);

    public static final int MASK_BOTH = maskOf(BOTH);
    public static final int MASK_THIRD_ONLY = maskOf(THIRD_ONLY);
    public static final int MASK_INHERIT = -1;

    private static final Map<String, Perspective> BY_NAME = new HashMap<>();

    static {
        for (Perspective perspective : values()) {
            BY_NAME.put(ModelParts.normalize(perspective.name()), perspective);
        }
        BY_NAME.put("first", FIRST_PERSON);
        BY_NAME.put("third", THIRD_PERSON);
    }

    public static final Codec<Perspective> CODEC = Codec.STRING.comapFlatMap(
        string -> {
            Perspective perspective = BY_NAME.get(ModelParts.normalize(string));
            return perspective != null
                ? DataResult.success(perspective)
                : DataResult.error(() -> "Unknown perspective: '" + string + "' (expected first_person or third_person)");
        },
        perspective -> perspective.name().toLowerCase(Locale.ROOT)
    );

    public static final Codec<List<Perspective>> LIST_CODEC = SingleOrList.of(CODEC);

    public static int maskOf(List<Perspective> perspectives) {
        int mask = 0;
        for (int i = 0; i < perspectives.size(); i++) mask |= 1 << perspectives.get(i).ordinal();
        return mask;
    }

    public static boolean masked(int mask, boolean firstPerson) {
        return (mask & (1 << (firstPerson ? FIRST_PERSON.ordinal() : THIRD_PERSON.ordinal()))) != 0;
    }
}
