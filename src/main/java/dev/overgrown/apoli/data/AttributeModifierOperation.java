package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;

import java.util.Map;

public enum AttributeModifierOperation {
    ADD_BASE_EARLY(Phase.BASE, 0,  (base, total, mod) -> new Result(base + mod, total)),
    MULTIPLY_BASE_ADDITIVE(Phase.BASE, 1, (base, total, mod) -> new Result(base + base * mod, total)),
    MULTIPLY_BASE_MULTIPLICATIVE(Phase.BASE, 2, (base, total, mod) -> new Result(base * (1.0 + mod), total)),
    STANDARD_MULTIPLY_BASE(Phase.BASE, 3, (base, total, mod) -> new Result(base * mod, total)),
    STANDARD_DIVIDE_BASE(Phase.BASE, 4, (base, total, mod) -> new Result(mod == 0 ? base : base / mod, total)),
    ADD_BASE_LATE(Phase.BASE, 5, (base, total, mod) -> new Result(base + mod, total)),
    MIN_BASE(Phase.BASE, 6, (base, total, mod) -> new Result(Math.max(base, mod), total)),
    MAX_BASE(Phase.BASE, 7, (base, total, mod) -> new Result(Math.min(base, mod), total)),
    SET_BASE(Phase.BASE, 8, (base, total, mod) -> new Result(mod, total)),

    ADD_TOTAL_EARLY(Phase.TOTAL, 0, (base, total, mod) -> new Result(base, total + mod)),
    MULTIPLY_TOTAL_ADDITIVE(Phase.TOTAL, 1, (base, total, mod) -> new Result(base, total + total * mod)),
    MULTIPLY_TOTAL_MULTIPLICATIVE(Phase.TOTAL, 2, (base, total, mod) -> new Result(base, total * (1.0 + mod))),
    STANDARD_MULTIPLY_TOTAL(Phase.TOTAL, 3, (base, total, mod) -> new Result(base, total * mod)),
    STANDARD_DIVIDE_TOTAL(Phase.TOTAL, 4, (base, total, mod) -> new Result(base, mod == 0 ? total : total / mod)),
    MIN_TOTAL(Phase.TOTAL, 5, (base, total, mod) -> new Result(base, Math.max(total, mod))),
    MAX_TOTAL(Phase.TOTAL, 6, (base, total, mod) -> new Result(base, Math.min(total, mod))),
    SET_TOTAL(Phase.TOTAL, 7, (base, total, mod) -> new Result(base, mod)),
    ADD_TOTAL_LATE(Phase.TOTAL, 8, (base, total, mod) -> new Result(base, total + mod));

    public enum Phase {
        BASE,
        TOTAL
    }

    public record Result(double base, double total) {}

    @FunctionalInterface
    public interface Apply {
        Result apply(double base, double total, double modifier);
    }

    private static final Map<String, AttributeModifierOperation> ALIASES = Map.of(
        "addition", ADD_BASE_EARLY,
        "multiply_base", MULTIPLY_BASE_ADDITIVE,
        "multiply_total", MULTIPLY_TOTAL_MULTIPLICATIVE,
        "ADD_VALUE", ADD_BASE_EARLY,
        "ADD_MULTIPLIED_BASE", MULTIPLY_BASE_ADDITIVE,
        "ADD_MULTIPLIED_TOTAL", MULTIPLY_TOTAL_MULTIPLICATIVE
    );

    public static final Codec<AttributeModifierOperation> CODEC = Codec.STRING.flatXmap(
        s -> {

            String name = s.startsWith("apoli:") ? s.substring("apoli:".length()) : s;
            AttributeModifierOperation v = ALIASES.get(name);
            if (v != null) return com.mojang.serialization.DataResult.success(v);
            for (AttributeModifierOperation op : values()) {
                if (op.serializedName().equals(name)) return com.mojang.serialization.DataResult.success(op);
            }
            return com.mojang.serialization.DataResult.error(() -> "Unknown attribute modifier operation: " + s);
        },
        op -> com.mojang.serialization.DataResult.success(op.serializedName())
    );

    private final Phase phase;
    private final int order;
    private final Apply applyFn;

    AttributeModifierOperation(Phase phase, int order, Apply apply) {
        this.phase = phase;
        this.order = order;
        this.applyFn = apply;
    }

    public Phase phase() {
        return phase;
    }

    public int order() {
        return order;
    }

    public String serializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public Result apply(double base, double total, double modifier) {
        return applyFn.apply(base, total, modifier);
    }

    public double applySingle(double value, double modifier) {
        Result r = applyFn.apply(value, value, modifier);
        return phase == Phase.BASE ? r.base() : r.total();
    }

    public net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation vanillaOperation() {
        return switch (this) {
            case ADD_BASE_EARLY, ADD_BASE_LATE, SET_BASE, ADD_TOTAL_EARLY, ADD_TOTAL_LATE ->
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
            case MULTIPLY_BASE_ADDITIVE, MULTIPLY_BASE_MULTIPLICATIVE,
                 STANDARD_MULTIPLY_BASE, STANDARD_DIVIDE_BASE,
                 MIN_BASE, MAX_BASE ->
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case MULTIPLY_TOTAL_ADDITIVE, MULTIPLY_TOTAL_MULTIPLICATIVE,
                 STANDARD_MULTIPLY_TOTAL, STANDARD_DIVIDE_TOTAL,
                 MIN_TOTAL, MAX_TOTAL, SET_TOTAL ->
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }
}
