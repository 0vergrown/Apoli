package dev.overgrown.apoli.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModifierSummary {
    private final double added;
    private final double multipliedBase;
    private final double[] multipliedTotal;
    private volatile Scaled scaled;

    private record Scaled(double multiplier, List<AttributeModifier> modifiers) {}

    public ModifierSummary(double added, double multipliedBase, double[] multipliedTotal) {
        this.added = added;
        this.multipliedBase = multipliedBase;
        this.multipliedTotal = multipliedTotal;
    }

    public List<AttributeModifier> modifiers(double multiplier) {
        Scaled current = this.scaled;
        if (current != null && Double.compare(current.multiplier(), multiplier) == 0) return current.modifiers();
        List<AttributeModifier> built = build(multiplier);
        this.scaled = new Scaled(multiplier, built);
        return built;
    }

    private List<AttributeModifier> build(double multiplier) {
        List<AttributeModifier> out = new ArrayList<>(3);
        if (added != 0.0) out.add(constant(AttributeModifierOperation.ADD_BASE_EARLY, added * multiplier));
        if (multipliedBase != 0.0) {
            out.add(constant(AttributeModifierOperation.MULTIPLY_BASE_MULTIPLICATIVE, multipliedBase * multiplier));
        }
        double product = 1.0;
        for (double amount : multipliedTotal) product *= 1.0 + amount * multiplier;
        if (product != 1.0) out.add(constant(AttributeModifierOperation.MULTIPLY_TOTAL_MULTIPLICATIVE, product - 1.0));
        return List.copyOf(out);
    }

    private static AttributeModifier constant(AttributeModifierOperation operation, double value) {
        return new AttributeModifier(operation, Expression.constant(value), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());
    }
}
