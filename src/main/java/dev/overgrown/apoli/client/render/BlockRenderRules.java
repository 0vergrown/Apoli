package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ModifyBlockRenderPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class BlockRenderRules {

    public record Rule(@Nullable BlockCondition condition, BlockState replacement) {}

    private static final Rule[] NONE = new Rule[0];

    private static volatile Rule[] rules = NONE;

    private BlockRenderRules() {}

    public static boolean active() {
        return rules.length != 0;
    }

    public static BlockState replace(Level level, BlockPos pos, BlockState original) {
        Rule[] snapshot = rules;
        if (snapshot.length == 0) return original;
        BlockCtx ctx = null;
        for (int i = 0; i < snapshot.length; i++) {
            Rule rule = snapshot[i];
            if (rule.condition() != null) {
                if (ctx == null) ctx = new BlockCtx(pos, original, level);
                if (!rule.condition().test(ctx)) continue;
            }
            return rule.replacement();
        }
        return original;
    }

    public static void clientTick(Minecraft client) {
        Rule[] next = build(client);
        if (same(rules, next)) return;
        rules = next;
        if (client.levelRenderer != null && client.level != null) client.levelRenderer.allChanged();
    }

    public static void clear() {
        rules = NONE;
    }

    private static Rule[] build(Minecraft client) {
        if (client.player == null || client.level == null) return NONE;
        List<Rule> collected = new ArrayList<>(2);
        PowerLookup.forEach(client.player, ApoliIds.MODIFY_BLOCK_RENDER, ModifyBlockRenderPower.Config.class, cfg -> {
            Block block = BuiltInRegistries.BLOCK.get(cfg.block());
            if (block == null) return;
            collected.add(new Rule(cfg.blockCondition().orElse(null), block.defaultBlockState()));
        });
        return collected.isEmpty() ? NONE : collected.toArray(Rule[]::new);
    }

    private static boolean same(Rule[] current, Rule[] next) {
        if (current.length != next.length) return false;
        for (int i = 0; i < current.length; i++) {
            if (current[i].condition() != next[i].condition()) return false;
            if (current[i].replacement() != next[i].replacement()) return false;
        }
        return true;
    }
}
