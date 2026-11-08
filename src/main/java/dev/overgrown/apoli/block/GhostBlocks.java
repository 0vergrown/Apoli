package dev.overgrown.apoli.block;

import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.condition.context.BlockCtx;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public final class GhostBlocks {

    public static final int PLACE_FLAGS =
        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static final long AWAITING_CHUNK = -1L;
    private static final long RESTORED = -2L;
    private static final int RELOAD_CHECK_INTERVAL = 20;
    private static final int MAX_SHUTDOWN_PASSES = 8;

    private GhostBlocks() {}

    private static final class Ghost {
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        final BlockState original;
        @Nullable final CompoundTag originalBlockEntity;
        Optional<BlockAction> endAction = Optional.empty();
        long expiresAt = RESTORED;

        Ghost(ResourceKey<Level> dimension, BlockPos pos, BlockState original,
              @Nullable CompoundTag originalBlockEntity) {
            this.dimension = dimension;
            this.pos = pos;
            this.original = original;
            this.originalBlockEntity = originalBlockEntity;
        }
    }

    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Ghost>> ACTIVE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<ArrayList<Ghost>>> WAITING_FOR_CHUNK =
        new HashMap<>();
    private static final Long2ObjectOpenHashMap<ArrayList<Ghost>> EXPIRY = new Long2ObjectOpenHashMap<>();
    private static long currentTick;

    public static boolean isGhost(Level level, BlockPos pos) {
        if (ACTIVE.isEmpty() || level.isClientSide()) return false;
        Long2ObjectOpenHashMap<Ghost> ghosts = ACTIVE.get(level.dimension());
        return ghosts != null && ghosts.containsKey(pos.asLong());
    }

    public static void place(ServerLevel level, BlockPos pos, BlockState state,
                             @Nullable CompoundTag blockEntityNbt, int duration,
                             Optional<BlockAction> blockAction, Optional<BlockAction> endAction) {
        if (!chunkLoaded(level, pos)) return;
        ResourceKey<Level> dimension = level.dimension();
        long packed = pos.asLong();
        Long2ObjectOpenHashMap<Ghost> ghosts = ACTIVE.get(dimension);
        if (ghosts == null) {
            ghosts = new Long2ObjectOpenHashMap<>();
            ACTIVE.put(dimension, ghosts);
        }
        Ghost ghost = ghosts.get(packed);
        if (ghost == null) {
            BlockEntity be = level.getBlockEntity(pos);
            ghost = new Ghost(dimension, pos.immutable(), level.getBlockState(pos),
                be == null ? null : be.saveWithFullMetadata());
            ghosts.put(packed, ghost);
        }

        level.setBlock(pos, state, PLACE_FLAGS);
        if (blockEntityNbt != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                CompoundTag tag = blockEntityNbt.copy();
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());
                be.load(tag);
                be.setChanged();
            }
        }

        ghost.endAction = endAction;
        schedule(ghost, currentTick + Math.max(1, duration));

        if (blockAction.isPresent()) {
            blockAction.get().run(new BlockCtx(ghost.pos, state, level));
        }
    }

    public static void tick(MinecraftServer server) {
        long now = ++currentTick;
        if (!EXPIRY.isEmpty()) {
            ArrayList<Ghost> due = EXPIRY.remove(now);
            if (due != null) {
                for (int i = 0, n = due.size(); i < n; i++) {
                    Ghost ghost = due.get(i);
                    if (ghost.expiresAt == now) expire(server, ghost);
                }
            }
        }
        if (!WAITING_FOR_CHUNK.isEmpty() && now % RELOAD_CHECK_INTERVAL == 0) restoreReloadedChunks(server);
    }

    public static void restoreAll(MinecraftServer server) {
        for (int pass = 0; pass < MAX_SHUTDOWN_PASSES && !ACTIVE.isEmpty(); pass++) {
            ArrayList<Ghost> remaining = new ArrayList<>();
            for (Long2ObjectOpenHashMap<Ghost> ghosts : ACTIVE.values()) remaining.addAll(ghosts.values());
            ACTIVE.clear();
            WAITING_FOR_CHUNK.clear();
            EXPIRY.clear();
            for (int i = 0, n = remaining.size(); i < n; i++) {
                Ghost ghost = remaining.get(i);
                ghost.expiresAt = RESTORED;
                ServerLevel level = server.getLevel(ghost.dimension);
                if (level == null) continue;
                int chunkX = SectionPos.blockToSectionCoord(ghost.pos.getX());
                int chunkZ = SectionPos.blockToSectionCoord(ghost.pos.getZ());
                if (level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true) == null) continue;
                putBack(level, ghost);
            }
        }
        ACTIVE.clear();
        WAITING_FOR_CHUNK.clear();
        EXPIRY.clear();
    }

    public static void clear() {
        ACTIVE.clear();
        WAITING_FOR_CHUNK.clear();
        EXPIRY.clear();
        currentTick = 0;
    }

    private static void schedule(Ghost ghost, long tick) {
        ghost.expiresAt = tick;
        ArrayList<Ghost> due = EXPIRY.get(tick);
        if (due == null) {
            due = new ArrayList<>(2);
            EXPIRY.put(tick, due);
        }
        due.add(ghost);
    }

    private static void expire(MinecraftServer server, Ghost ghost) {
        ServerLevel level = server.getLevel(ghost.dimension);
        if (level == null) {
            forget(ghost);
            return;
        }
        if (!chunkLoaded(level, ghost.pos)) {
            waitForChunk(ghost);
            return;
        }
        forget(ghost);
        putBack(level, ghost);
    }

    private static void waitForChunk(Ghost ghost) {
        ghost.expiresAt = AWAITING_CHUNK;
        Long2ObjectOpenHashMap<ArrayList<Ghost>> chunks = WAITING_FOR_CHUNK.get(ghost.dimension);
        if (chunks == null) {
            chunks = new Long2ObjectOpenHashMap<>();
            WAITING_FOR_CHUNK.put(ghost.dimension, chunks);
        }
        long chunk = ChunkPos.asLong(
            SectionPos.blockToSectionCoord(ghost.pos.getX()), SectionPos.blockToSectionCoord(ghost.pos.getZ()));
        ArrayList<Ghost> waiting = chunks.get(chunk);
        if (waiting == null) {
            waiting = new ArrayList<>(2);
            chunks.put(chunk, waiting);
        }
        waiting.add(ghost);
    }

    private static void restoreReloadedChunks(MinecraftServer server) {
        for (Iterator<Map.Entry<ResourceKey<Level>, Long2ObjectOpenHashMap<ArrayList<Ghost>>>> dimensions =
                 WAITING_FOR_CHUNK.entrySet().iterator(); dimensions.hasNext(); ) {
            Map.Entry<ResourceKey<Level>, Long2ObjectOpenHashMap<ArrayList<Ghost>>> dimension = dimensions.next();
            ServerLevel level = server.getLevel(dimension.getKey());
            Long2ObjectOpenHashMap<ArrayList<Ghost>> chunks = dimension.getValue();
            for (ObjectIterator<Long2ObjectMap.Entry<ArrayList<Ghost>>> it =
                     chunks.long2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
                Long2ObjectMap.Entry<ArrayList<Ghost>> entry = it.next();
                long chunk = entry.getLongKey();
                ArrayList<Ghost> waiting = entry.getValue();
                if (level != null
                    && level.getChunkSource().getChunkNow(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)) == null) {
                    continue;
                }
                it.remove();
                for (int i = 0, n = waiting.size(); i < n; i++) {
                    Ghost ghost = waiting.get(i);
                    if (ghost.expiresAt != AWAITING_CHUNK) continue;
                    forget(ghost);
                    if (level != null) putBack(level, ghost);
                }
            }
            if (chunks.isEmpty()) dimensions.remove();
        }
    }

    private static void forget(Ghost ghost) {
        ghost.expiresAt = RESTORED;
        Long2ObjectOpenHashMap<Ghost> ghosts = ACTIVE.get(ghost.dimension);
        if (ghosts == null) return;
        ghosts.remove(ghost.pos.asLong(), ghost);
        if (ghosts.isEmpty()) ACTIVE.remove(ghost.dimension);
    }

    private static void putBack(ServerLevel level, Ghost ghost) {
        BlockPos pos = ghost.pos;
        level.setBlock(pos, ghost.original, PLACE_FLAGS);
        if (ghost.originalBlockEntity != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.load(ghost.originalBlockEntity);
                be.setChanged();
            }
        }
        level.levelEvent(2001, pos, Block.getId(ghost.original));

        if (ghost.endAction.isPresent()) {
            ghost.endAction.get().run(new BlockCtx(pos, ghost.original, level));
        }
    }

    private static boolean chunkLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(
            SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) != null;
    }
}
