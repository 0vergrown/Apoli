package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ModifySlipperinessHandler {
    private ModifySlipperinessHandler() {}

    public static float modify(LivingEntity entity, BlockPos affectingPos, float original) {
        Level level = entity.level();
        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(entity, Apoli.id("modify_slipperiness"), ModifySlipperinessPower.Config.class, cfg -> {
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
