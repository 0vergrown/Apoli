package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;

import java.util.List;

public enum Hand implements StringRepresentable {
    MAIN_HAND("main_hand", InteractionHand.MAIN_HAND),
    OFF_HAND("off_hand", InteractionHand.OFF_HAND);

    public static final Codec<Hand> CODEC = Codec.STRING.comapFlatMap(Hand::byName, Hand::getSerializedName);
    public static final Codec<List<Hand>> LIST_CODEC = Codec.list(CODEC);
    public static final List<Hand> BOTH = List.of(OFF_HAND, MAIN_HAND);

    private final String name;
    private final InteractionHand vanilla;

    Hand(String name, InteractionHand vanilla) {
        this.name = name;
        this.vanilla = vanilla;
    }

    public InteractionHand vanilla() {
        return vanilla;
    }

    public static Hand of(InteractionHand vanilla) {
        return vanilla == InteractionHand.MAIN_HAND ? MAIN_HAND : OFF_HAND;
    }

    public static DataResult<Hand> byName(String name) {
        return switch (name) {
            case "main_hand", "mainhand" -> DataResult.success(MAIN_HAND);
            case "off_hand", "offhand" -> DataResult.success(OFF_HAND);
            default -> DataResult.error(() -> "Unknown hand: '" + name
                + "' (expected main_hand/mainhand or off_hand/offhand)");
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
