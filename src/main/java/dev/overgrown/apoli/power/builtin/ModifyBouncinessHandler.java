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
    private ModifyBouncinessHandler() {}

    public static double modify(LivingEntity entity, double original, BlockCtx block) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return 0;
        if (container.powersOfType(ApoliIds.MODIFY_BOUNCINESS).isEmpty()) return 0;

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

    public static boolean damage(LivingEntity entity, BlockCtx block) {
        List<Boolean> result = new ArrayList<>();
        result.add(0, false);

        PowerLookup.forEach(entity, ApoliIds.MODIFY_BOUNCINESS, ModifyBouncinessPower.Config.class, cfg -> {
            if (cfg.blockCondition().isEmpty() || cfg.blockCondition().get().test(block)) {
                if (cfg.damage()) {
                    result.set(0, true);
                }
            }
        });

        return result.get(0);
    }

    public static boolean preventable(LivingEntity entity, BlockCtx block) {
        List<Boolean> result = new ArrayList<>();
        result.add(0, false);

        PowerLookup.forEach(entity, ApoliIds.MODIFY_BOUNCINESS, ModifyBouncinessPower.Config.class, cfg -> {
            if (cfg.blockCondition().isEmpty() || cfg.blockCondition().get().test(block)) {
                if (cfg.preventable()) {
                    result.set(0, true);
                }
            }
        });

        return result.get(0);
    }
}
