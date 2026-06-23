package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum Comparison implements StringRepresentable {
    LESS("<", (a, b) -> a < b),
    LESS_EQUAL("<=", (a, b) -> a <= b),
    GREATER(">", (a, b) -> a > b),
    GREATER_EQUAL(">=", (a, b) -> a >= b),
    EQUAL("==", (a, b) -> a == b),
    NOT_EQUAL("!=", (a, b) -> a != b);

    public static final Codec<Comparison> CODEC = StringRepresentable.fromEnum(Comparison::values);

    private final String symbol;
    private final DoubleComparator op;

    Comparison(String symbol, DoubleComparator op) {
        this.symbol = symbol;
        this.op = op;
    }

    public boolean compare(double a, double b) {
        return op.test(a, b);
    }

    @Override
    public String getSerializedName() {
        return symbol;
    }

    @FunctionalInterface
    private interface DoubleComparator {
        boolean test(double a, double b);
    }
}