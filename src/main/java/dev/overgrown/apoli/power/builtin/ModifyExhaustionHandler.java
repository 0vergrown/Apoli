package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierOperation;
import dev.overgrown.apoli.data.ExpressionContext;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ModifyExhaustionHandler {
    private ModifyExhaustionHandler() {}

    public static float modify(Player player, float exhaustion) {
        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(player, Apoli.id("modify_exhaustion"), ModifyExhaustionPower.Config.class, cfg -> {
            cfg.modifier().ifPresent(mods::add);
            cfg.modifiers().ifPresent(mods::addAll);
        });
        if (mods.isEmpty()) return exhaustion;

        PowerContainer container = PowerContainer.of(player);
        Map<String, Double> vars = ExpressionContext.entityStats(player);
        Map<String, Double> resources = container != null
            ? ExpressionContext.resourceValues(container) : Map.of();

        mods.sort(Comparator
            .comparingInt((AttributeModifier m) -> m.operation().phase().ordinal())
            .thenComparingInt(m -> m.operation().order()));

        double base = exhaustion;
        double total = exhaustion;
        for (AttributeModifier mod : mods) {
            double modValue = mod.resolveInput(vars, resources);
            AttributeModifierOperation.Result r = mod.operation().apply(base, total, modValue);
            base = r.base();
            total = r.total();
            if (mod.operation().phase() == AttributeModifierOperation.Phase.BASE) {
                total = base;
            }
        }
        return Math.max(0f, (float) total);
    }
}
