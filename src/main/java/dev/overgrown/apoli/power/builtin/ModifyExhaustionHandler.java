package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class ModifyExhaustionHandler {
    private ModifyExhaustionHandler() {}

    public static float modify(Player player, float exhaustion) {
        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(player, ApoliIds.MODIFY_EXHAUSTION, ModifyExhaustionPower.Config.class, cfg -> {
            cfg.modifier().ifPresent(mods::add);
            cfg.modifiers().ifPresent(mods::addAll);
        });
        if (mods.isEmpty()) return exhaustion;
        return Math.max(0f, AttributeModifierHelper.apply(exhaustion, mods, player));
    }
}
