package dev.overgrown.apoli.data;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AttributeModifierHelper {
    private AttributeModifierHelper() {}

    public static List<AttributeModifier> flatten(Optional<AttributeModifier> single,
                                                   Optional<List<AttributeModifier>> multi) {
        if (single.isEmpty() && multi.isEmpty()) return List.of();
        if (single.isPresent() && multi.isEmpty()) return List.of(single.get());
        if (single.isEmpty()) return multi.get();
        List<AttributeModifier> combined = new ArrayList<>(multi.get().size() + 1);
        combined.add(single.get());
        combined.addAll(multi.get());
        return combined;
    }

    public static float apply(float baseValue, List<AttributeModifier> mods, LivingEntity entity) {
        if (mods.isEmpty()) return baseValue;
        Map<String, Double> vars = entity != null ? ExpressionContext.entityStats(entity) : Map.of();
        Map<String, Double> resources = entity != null
            ? ExpressionContext.resourceValues(PowerContainer.of(entity))
            : Map.of();
        double base = baseValue;
        double total = baseValue;
        List<AttributeModifier> sorted = new ArrayList<>(mods);
        sorted.sort(Comparator
            .comparingInt((AttributeModifier m) -> m.operation().phase().ordinal())
            .thenComparingInt(m -> m.operation().order()));
        for (AttributeModifier mod : sorted) {
            double modValue = mod.resolveInput(vars, resources);
            AttributeModifierOperation.Result r = mod.operation().apply(base, total, modValue);
            base = r.base();
            total = r.total();
            if (mod.operation().phase() == AttributeModifierOperation.Phase.BASE) total = base;
        }
        return (float) total;
    }
}
