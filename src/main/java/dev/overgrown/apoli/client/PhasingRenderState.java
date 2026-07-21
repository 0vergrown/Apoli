package dev.overgrown.apoli.client;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PhasingPower;
import dev.overgrown.apoli.power.ApoliIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class PhasingRenderState {
    private PhasingRenderState() {}

    private static final int FLAGS = Block.UPDATE_ALL;

    private static final Map<BlockPos, BlockState> HIDDEN = new HashMap<>();

    private static ClientLevel boundLevel = null;

    public static void clientTick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level != boundLevel) {
            HIDDEN.clear();
            boundLevel = level;
        }
        if (level == null) return;

        Entity camera = mc.getCameraEntity();
        if (!(camera instanceof LivingEntity living) || !hasRemoveBlocksPhasing(living)) {
            restoreAll(level);
            return;
        }

        Set<BlockPos> eye = eyePositions(living);

        Iterator<Map.Entry<BlockPos, BlockState>> it = HIDDEN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, BlockState> e = it.next();
            if (!eye.contains(e.getKey())) {
                restoreOne(level, e.getKey(), e.getValue());
                it.remove();
            }
        }
        for (BlockPos pos : eye) {
            if (HIDDEN.containsKey(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            HIDDEN.put(pos, state);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
    }

    private static boolean hasRemoveBlocksPhasing(LivingEntity living) {
        boolean[] found = {false};
        PowerLookup.forEach(living, ApoliIds.PHASING, PhasingPower.Config.class, cfg -> {
            if (cfg.renderType() == PhasingPower.RenderType.REMOVE_BLOCKS) found[0] = true;
        });
        return found[0];
    }

    private static Set<BlockPos> eyePositions(LivingEntity living) {
        Vec3 eye = living.getEyePosition();
        AABB box = new AABB(eye, eye).inflate(0.25, 0.05, 0.25);
        Set<BlockPos> set = new HashSet<>();
        BlockPos.betweenClosedStream(box).forEach(p -> set.add(p.immutable()));
        return set;
    }

    private static void restoreOne(ClientLevel level, BlockPos pos, BlockState original) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, original, FLAGS);
        }
    }

    private static void restoreAll(ClientLevel level) {
        if (HIDDEN.isEmpty()) return;
        for (Map.Entry<BlockPos, BlockState> e : HIDDEN.entrySet()) {
            restoreOne(level, e.getKey(), e.getValue());
        }
        HIDDEN.clear();
    }
}
