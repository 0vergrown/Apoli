package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public final class ModifyJumpHandler {
    private ModifyJumpHandler() {}

    private static boolean hasNone(LivingEntity entity) {
        PowerContainer container = PowerContainer.of(entity);
        return container == null || container.isEmpty()
            || container.powersOfType(ApoliIds.MODIFY_JUMP).isEmpty();
    }

    public static float modify(LivingEntity entity, float original) {
        if (hasNone(entity)) return original;
        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(entity, ApoliIds.MODIFY_JUMP, ModifyJumpPower.Config.class, cfg ->
            mods.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers())));
        if (mods.isEmpty()) return original;
        return AttributeModifierHelper.apply(original, AttributeModifierHelper.ensureSorted(mods), entity);
    }

    public static void onJump(LivingEntity entity) {
        if (hasNone(entity)) return;
        EntityCtx ctx = EntityCtx.of(entity, entity.level());
        PowerLookup.forEach(entity, ApoliIds.MODIFY_JUMP, ModifyJumpPower.Config.class, cfg ->
            cfg.entityAction().ifPresent(action -> action.run(ctx)));
    }
}
