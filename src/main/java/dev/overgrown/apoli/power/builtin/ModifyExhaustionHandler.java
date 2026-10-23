package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class ModifyExhaustionHandler {
    private ModifyExhaustionHandler() {}

    public static float modify(Player player, float exhaustion) {
        PowerContainer container = PowerContainer.of(player);
        if (container == null || container.isEmpty()) return exhaustion;
        boolean own = !container.powersOfType(ApoliIds.MODIFY_EXHAUSTION).isEmpty();
        boolean transferred = AttributeModifyTransferPower.EXHAUSTION.appliesTo(container);
        if (!own && !transferred) return exhaustion;
        List<AttributeModifier> mods = List.of();
        if (own) {
            List<AttributeModifier> collected = new ArrayList<>();
            PowerLookup.forEach(player, ApoliIds.MODIFY_EXHAUSTION, ModifyExhaustionPower.Config.class, cfg -> {
                cfg.modifier().ifPresent(collected::add);
                cfg.modifiers().ifPresent(collected::addAll);
            });
            mods = collected;
        }
        if (transferred) mods = AttributeModifyTransferPower.EXHAUSTION.merge(player, container, mods);
        if (mods.isEmpty()) return exhaustion;
        return Math.max(0f, AttributeModifierHelper.apply(exhaustion, mods, player));
    }
}
