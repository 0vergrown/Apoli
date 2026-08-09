package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class ModifyBreakSpeedHandler {
    private ModifyBreakSpeedHandler() {}

    public static boolean hasNone(Player player) {
        PowerContainer container = PowerContainer.of(player);
        return container == null || container.isEmpty()
            || container.powersOfType(ApoliIds.MODIFY_BREAK_SPEED).isEmpty();
    }

    public static float modifyHardness(float hardness, Player player, BlockGetter level,
                                       BlockPos pos, BlockState state) {
        if (hasNone(player)) return hardness;
        List<AttributeModifier> mods = collect(player, level, pos, state, true);
        if (mods.isEmpty()) return hardness;
        return (float) Math.max(AttributeModifierHelper.apply((double) hardness, mods, player), -1.0D);
    }

    public static float modifySpeed(float speed, Player player, BlockGetter level,
                                    BlockPos pos, BlockState state) {
        if (hasNone(player)) return speed;
        List<AttributeModifier> mods = collect(player, level, pos, state, false);
        if (mods.isEmpty()) return speed;
        return AttributeModifierHelper.apply(speed, mods, player);
    }

    private static List<AttributeModifier> collect(Player player, BlockGetter level, BlockPos pos,
                                                   BlockState state, boolean hardness) {
        BlockCtx ctx = new BlockCtx(pos, state, player.level());
        List<AttributeModifier> out = new ArrayList<>(2);
        PowerLookup.forEach(player, ApoliIds.MODIFY_BREAK_SPEED, ModifyBreakSpeedPower.Config.class, cfg -> {
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
            out.addAll(hardness
                ? AttributeModifierHelper.flatten(cfg.hardnessModifier(), cfg.hardnessModifiers())
                : AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()));
        });
        return AttributeModifierHelper.ensureSorted(out);
    }
}
