package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public final class ModifyBouncinessHandler {

    private static final int MATCHED = 1;
    private static final int DAMAGE = 2;
    private static final int PREVENTABLE = 4;

    private ModifyBouncinessHandler() {}

    public static boolean has(LivingEntity entity) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        return !container.powersOfType(ApoliIds.MODIFY_BOUNCINESS).isEmpty();
    }

    public static double modify(LivingEntity entity, double original, BlockCtx block) {
        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(entity, ApoliIds.MODIFY_BOUNCINESS, ModifyBouncinessPower.Config.class, cfg -> {
            if (cfg.blockCondition().isEmpty() || cfg.blockCondition().get().test(block)) {
                cfg.modifier().ifPresent(mods::add);
                cfg.modifiers().ifPresent(mods::addAll);
            }
        });
        if (mods.isEmpty()) return 0;
        return Math.max(0.0, AttributeModifierHelper.apply(original, AttributeModifierHelper.ensureSorted(mods), entity));
    }

    public static boolean preventsFallDamage(LivingEntity entity, BlockCtx block) {
        int flags = flags(entity, block);
        return (flags & MATCHED) != 0 && (flags & DAMAGE) == 0;
    }

    public static boolean preventable(LivingEntity entity, BlockCtx block) {
        return (flags(entity, block) & PREVENTABLE) != 0;
    }

    private static int flags(LivingEntity entity, BlockCtx block) {
        int[] flags = new int[1];
        PowerLookup.forEach(entity, ApoliIds.MODIFY_BOUNCINESS, ModifyBouncinessPower.Config.class, cfg -> {
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(block)) return;
            flags[0] |= MATCHED;
            if (cfg.damage()) flags[0] |= DAMAGE;
            if (cfg.preventable()) flags[0] |= PREVENTABLE;
        });
        return flags[0];
    }
}
