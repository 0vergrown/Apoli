package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.codec.SingleOrList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum CameraPerspective {
    FIRST_PERSON,
    THIRD_PERSON_BACK,
    THIRD_PERSON_FRONT;

    private static final CameraPerspective[] VALUES = values();
    private static final Map<String, CameraPerspective> BY_NAME = new HashMap<>();
    private static final Map<String, Integer> MASKS = new HashMap<>();

    public static final int MASK_THIRD = bit(THIRD_PERSON_BACK) | bit(THIRD_PERSON_FRONT);

    static {
        for (CameraPerspective perspective : VALUES) {
            BY_NAME.put(ModelParts.normalize(perspective.name()), perspective);
            MASKS.put(ModelParts.normalize(perspective.name()), bit(perspective));
        }
        BY_NAME.put("first", FIRST_PERSON);
        BY_NAME.put("third", THIRD_PERSON_BACK);
        BY_NAME.put("thirdperson", THIRD_PERSON_BACK);
        BY_NAME.put("back", THIRD_PERSON_BACK);
        BY_NAME.put("front", THIRD_PERSON_FRONT);
        MASKS.put("first", bit(FIRST_PERSON));
        MASKS.put("third", MASK_THIRD);
        MASKS.put("thirdperson", MASK_THIRD);
        MASKS.put("back", bit(THIRD_PERSON_BACK));
        MASKS.put("front", bit(THIRD_PERSON_FRONT));
    }

    public static final Codec<CameraPerspective> CODEC = Codec.STRING.comapFlatMap(
        string -> {
            CameraPerspective perspective = BY_NAME.get(ModelParts.normalize(string));
            return perspective != null
                ? DataResult.success(perspective)
                : DataResult.error(() -> "Unknown perspective: '" + string
                    + "' (expected first_person, third_person_back or third_person_front)");
        },
        perspective -> perspective.name().toLowerCase(Locale.ROOT)
    );

    private static final Codec<Integer> SINGLE_MASK = Codec.STRING.comapFlatMap(
        string -> {
            Integer mask = MASKS.get(ModelParts.normalize(string));
            return mask != null
                ? DataResult.success(mask)
                : DataResult.error(() -> "Unknown perspective: '" + string
                    + "' (expected first_person, third_person, third_person_back or third_person_front)");
        },
        mask -> nameOf(mask)
    );

    public static final Codec<Integer> MASK_CODEC = SingleOrList.of(SINGLE_MASK).xmap(
        masks -> {
            int combined = 0;
            for (int mask : masks) combined |= mask;
            return combined;
        },
        CameraPerspective::split
    );

    public int id() {
        return ordinal();
    }

    public boolean matches(int mask) {
        return (mask & bit(this)) != 0;
    }

    public static CameraPerspective byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : FIRST_PERSON;
    }

    public static int bit(CameraPerspective perspective) {
        return 1 << perspective.ordinal();
    }

    private static String nameOf(int mask) {
        if (mask == MASK_THIRD) return "third_person";
        for (CameraPerspective perspective : VALUES) {
            if (mask == bit(perspective)) return perspective.name().toLowerCase(Locale.ROOT);
        }
        return FIRST_PERSON.name().toLowerCase(Locale.ROOT);
    }

    private static List<Integer> split(int mask) {
        List<Integer> out = new ArrayList<>(3);
        if ((mask & MASK_THIRD) == MASK_THIRD) {
            out.add(MASK_THIRD);
            mask &= ~MASK_THIRD;
        }
        for (CameraPerspective perspective : VALUES) {
            if ((mask & bit(perspective)) != 0) out.add(bit(perspective));
        }
        return out;
    }
}
