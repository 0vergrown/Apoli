package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerTypeGate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ModifySlipperinessHandler {
    private static final PowerTypeGate IN_USE = new PowerTypeGate(ApoliIds.MODIFY_SLIPPERINESS);

    private ModifySlipperinessHandler() {}

    public static boolean applies(LivingEntity entity) {
        return IN_USE.inUse() && PowerLookup.holdsType(entity, ApoliIds.MODIFY_SLIPPERINESS);
    }

    public static float modify(LivingEntity entity, BlockPos affectingPos, float original) {
        Level level = entity.level();
        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(entity, ApoliIds.MODIFY_SLIPPERINESS, ModifySlipperinessPower.Config.class, cfg -> {
            if (cfg.blockCondition().isPresent()) {
                BlockCtx ctx = new BlockCtx(affectingPos, level.getBlockState(affectingPos), level);
                if (!cfg.blockCondition().get().test(ctx)) {
                    return;
                }
            }
            cfg.modifier().ifPresent(mods::add);
            cfg.modifiers().ifPresent(mods::addAll);
        });
        if (mods.isEmpty()) {
            return original;
        }
        return AttributeModifierHelper.apply(original, mods, entity);
    }
}
