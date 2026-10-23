package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.data.ConnectedBlocks;
import dev.overgrown.apoli.data.expr.ExprContext;
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
    private static final int SLOT_COUNT = ExprContext.slot("count");
    private static final ThreadLocal<CountCache> COUNT_CACHE = ThreadLocal.withInitial(CountCache::new);

    private static final class CountCache {
        long time = Long.MIN_VALUE;
        long pos;
        int actor;
        BlockState state;
        ConnectedBlocks group;
        int count;
    }

    private ModifyBreakSpeedHandler() {}

    public static boolean hasNone(Player player) {
        PowerContainer container = PowerContainer.of(player);
        return container == null || container.isEmpty()
            || container.powersOfType(ApoliIds.MODIFY_BREAK_SPEED).isEmpty();
    }

    public static float modifyHardness(float hardness, Player player, BlockGetter level,
                                       BlockPos pos, BlockState state) {
        if (hasNone(player)) return hardness;
        double previousCount = ExprContext.get(SLOT_COUNT);
        try {
            List<AttributeModifier> mods = collect(player, level, pos, state, true);
            if (mods.isEmpty()) return hardness;
            return (float) Math.max(AttributeModifierHelper.apply((double) hardness, mods, player), -1.0D);
        } finally {
            ExprContext.pop(SLOT_COUNT, previousCount);
        }
    }

    public static float modifySpeed(float speed, Player player, BlockGetter level,
                                    BlockPos pos, BlockState state) {
        PowerContainer container = PowerContainer.of(player);
        if (container == null || container.isEmpty()) return speed;
        boolean own = !container.powersOfType(ApoliIds.MODIFY_BREAK_SPEED).isEmpty();
        boolean transferred = AttributeModifyTransferPower.BREAK_SPEED.appliesTo(container);
        if (!own && !transferred) return speed;
        double previousCount = ExprContext.get(SLOT_COUNT);
        try {
            List<AttributeModifier> mods = own ? collect(player, level, pos, state, false) : List.of();
            if (transferred) {
                mods = AttributeModifierHelper.ensureSorted(AttributeModifyTransferPower.BREAK_SPEED.merge(player, container, mods));
            }
            if (mods.isEmpty()) return speed;
            return AttributeModifierHelper.apply(speed, mods, player);
        } finally {
            ExprContext.pop(SLOT_COUNT, previousCount);
        }
    }

    private static int connectedCount(ConnectedBlocks group, Player player, BlockPos pos, BlockState state) {
        CountCache cache = COUNT_CACHE.get();
        long time = player.level().getGameTime();
        long key = pos.asLong();
        if (cache.time == time && cache.pos == key && cache.actor == player.getId() && cache.state == state
                && cache.group == group) {
            return cache.count;
        }
        int count = group.count(player.level(), pos, state, player);
        cache.time = time;
        cache.pos = key;
        cache.actor = player.getId();
        cache.state = state;
        cache.group = group;
        cache.count = count;
        return count;
    }

    private static List<AttributeModifier> collect(Player player, BlockGetter level, BlockPos pos,
                                                   BlockState state, boolean hardness) {
        BlockCtx ctx = new BlockCtx(pos, state, player.level());
        List<AttributeModifier> out = new ArrayList<>(2);
        boolean[] counted = {false};
        PowerLookup.forEach(player, ApoliIds.MODIFY_BREAK_SPEED, ModifyBreakSpeedPower.Config.class, cfg -> {
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
            if (cfg.connectedBlocks().isPresent() && !counted[0]) {
                counted[0] = true;
                ExprContext.push(SLOT_COUNT, connectedCount(cfg.connectedBlocks().get(), player, pos, state));
            }
            out.addAll(hardness
                ? AttributeModifierHelper.flatten(cfg.hardnessModifier(), cfg.hardnessModifiers())
                : AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()));
        });
        return AttributeModifierHelper.ensureSorted(out);
    }
}
