package dev.overgrown.apoli.data;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class AttributeModifierHelper {
    private AttributeModifierHelper() {}

    private static final Comparator<AttributeModifier> ORDERING = Comparator
        .comparingInt((AttributeModifier m) -> m.operation().phase().ordinal())
        .thenComparingInt(m -> m.operation().order());

    public static List<AttributeModifier> flatten(Optional<AttributeModifier> single,
                                                   Optional<List<AttributeModifier>> multi) {
        if (single.isEmpty() && multi.isEmpty()) return List.of();
        if (single.isPresent() && multi.isEmpty()) return List.of(single.get());
        if (single.isEmpty()) return ensureSorted(multi.get());
        List<AttributeModifier> combined = new ArrayList<>(multi.get().size() + 1);
        combined.add(single.get());
        combined.addAll(multi.get());
        return ensureSorted(combined);
    }

    public static List<AttributeModifier> ensureSorted(List<AttributeModifier> mods) {
        int n = mods.size();
        if (n < 2) return mods;
        for (int i = 1; i < n; i++) {
            if (ORDERING.compare(mods.get(i - 1), mods.get(i)) > 0) {
                AttributeModifier[] copy = mods.toArray(new AttributeModifier[0]);
                Arrays.sort(copy, ORDERING);
                return Arrays.asList(copy);
            }
        }
        return mods;
    }

    public static float apply(float baseValue, List<AttributeModifier> mods, @Nullable Entity entity) {
        return (float) apply((double) baseValue, mods, entity);
    }

    public static double apply(double baseValue, List<AttributeModifier> mods, @Nullable Entity entity) {
        if (mods.isEmpty()) return baseValue;
        PowerContainer container = null;
        if (entity != null) {
            for (int i = 0, n = mods.size(); i < n; i++) {
                if (mods.get(i).needsContainer()) {
                    container = PowerContainer.of(entity);
                    break;
                }
            }
        }
        return apply(baseValue, mods, entity, container);
    }

    public static double apply(double baseValue, List<AttributeModifier> mods,
                               @Nullable Entity entity, @Nullable PowerContainer container) {
        if (mods.isEmpty()) return baseValue;
        List<AttributeModifier> ordered = ensureSorted(mods);
        double base = baseValue;
        double total = baseValue;
        for (int i = 0, n = ordered.size(); i < n; i++) {
            AttributeModifier mod = ordered.get(i);
            double modValue = mod.resolveInput(entity, container, baseValue);
            AttributeModifierOperation.Result r = mod.operation().apply(base, total, modValue);
            base = r.base();
            total = r.total();
            if (mod.operation().phase() == AttributeModifierOperation.Phase.BASE) total = base;
        }
        return total;
    }
}
