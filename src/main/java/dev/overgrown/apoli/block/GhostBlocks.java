package dev.overgrown.apoli.block;

import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GhostBlocks {

    public static final int PLACE_FLAGS =
        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private GhostBlocks() {}

    private record Key(ResourceKey<Level> dimension, long pos) {}

    private record Ghost(
        ResourceKey<Level> dimension,
        BlockPos pos,
        BlockState original,
        @Nullable CompoundTag originalBlockEntity,
        Optional<BlockAction> endAction
    ) {}

    private static final Map<Key, Ghost> ACTIVE = new HashMap<>();
    private static final Map<Long, List<Key>> EXPIRY = new HashMap<>();
    private static long currentTick;

    public static boolean isGhost(Level level, BlockPos pos) {
        if (ACTIVE.isEmpty()) return false;
        return ACTIVE.containsKey(new Key(level.dimension(), pos.asLong()));
    }

    public static void place(ServerLevel level, BlockPos pos, BlockState state,
                             @Nullable CompoundTag blockEntityNbt, int duration,
                             Optional<BlockAction> blockAction, Optional<BlockAction> endAction) {
        Key key = new Key(level.dimension(), pos.asLong());
        Ghost existing = ACTIVE.remove(key);

        BlockState original;
        CompoundTag originalBlockEntity;
        if (existing != null) {
            original = existing.original;
            originalBlockEntity = existing.originalBlockEntity;
        } else {
            original = level.getBlockState(pos);
            BlockEntity be = level.getBlockEntity(pos);
            originalBlockEntity = be == null ? null : be.saveWithFullMetadata(level.registryAccess());
        }

        level.setBlock(pos, state, PLACE_FLAGS);
        if (blockEntityNbt != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                CompoundTag tag = blockEntityNbt.copy();
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());
                be.loadWithComponents(tag, level.registryAccess());
                be.setChanged();
            }
        }

        ACTIVE.put(key, new Ghost(level.dimension(), pos.immutable(), original, originalBlockEntity, endAction));
        EXPIRY.computeIfAbsent(currentTick + Math.max(1, duration), k -> new ArrayList<>(1)).add(key);

        if (blockAction.isPresent()) {
            blockAction.get().run(new BlockCtx(pos.immutable(), state, level));
        }
    }

    public static void tick(MinecraftServer server) {
        List<Key> due = EXPIRY.remove(++currentTick);
        if (due == null) return;
        for (int i = 0; i < due.size(); i++) {
            restore(server, due.get(i));
        }
    }

    public static void restoreAll(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        for (Key key : List.copyOf(ACTIVE.keySet())) {
            restore(server, key);
        }
        EXPIRY.clear();
    }

    public static void clear() {
        ACTIVE.clear();
        EXPIRY.clear();
        currentTick = 0;
    }

    private static void restore(MinecraftServer server, Key key) {
        Ghost ghost = ACTIVE.remove(key);
        if (ghost == null) return;
        ServerLevel level = server.getLevel(ghost.dimension);
        if (level == null) return;

        BlockPos pos = ghost.pos;
        if (level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true) == null) return;

        level.setBlock(pos, ghost.original, PLACE_FLAGS);
        if (ghost.originalBlockEntity != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.loadWithComponents(ghost.originalBlockEntity.copy(), level.registryAccess());
                be.setChanged();
            }
        }
        level.levelEvent(2001, pos, Block.getId(ghost.original));

        if (ghost.endAction.isPresent()) {
            ghost.endAction.get().run(new BlockCtx(pos, ghost.original, level));
        }
    }
}
