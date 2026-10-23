package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ConnectedBlocks(
    Optional<BlockCondition> blockCondition,
    boolean diagonal,
    boolean downward,
    int limit,
    Optional<BlockCondition> neighborCondition
) {
    public static final MapCodec<ConnectedBlocks> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(ConnectedBlocks::blockCondition),
        Codec.BOOL.optionalFieldOf("diagonal", true).forGetter(ConnectedBlocks::diagonal),
        Codec.BOOL.optionalFieldOf("downward", true).forGetter(ConnectedBlocks::downward),
        Codec.intRange(1, 1024).optionalFieldOf("limit", 64).forGetter(ConnectedBlocks::limit),
        LoggedOptionalField.strict("neighbor_condition", BlockCondition.CODEC).forGetter(ConnectedBlocks::neighborCondition)
    ).apply(i, ConnectedBlocks::new));

    public static final Codec<ConnectedBlocks> CODEC = MAP_CODEC.codec();

    private static final int[][] ALL = offsets(true, true);
    private static final int[][] LEVEL_AND_UP = offsets(true, false);
    private static final int[][] FACES = offsets(false, true);
    private static final int[][] FACES_NOT_DOWN = offsets(false, false);

    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private static final class Scratch {
        final LongOpenHashSet checked = new LongOpenHashSet();
        final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    }

    private static int[][] offsets(boolean diagonal, boolean downward) {
        List<int[]> out = new ArrayList<>(26);
        for (int dy = downward ? -1 : 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int axes = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
                    if (axes == 0 || (!diagonal && axes > 1)) continue;
                    out.add(new int[]{dx, dy, dz});
                }
            }
        }
        return out.toArray(new int[0][]);
    }

    public List<BlockPos> find(Level level, BlockPos start, BlockState startState, @Nullable Entity actor) {
        List<BlockPos> found = new ArrayList<>();
        int size = walk(level, start, startState, actor, new LongOpenHashSet(), new LongArrayFIFOQueue(), found);
        return size == 0 ? List.of() : found;
    }

    public int count(Level level, BlockPos start, BlockState startState, @Nullable Entity actor) {
        Scratch scratch = SCRATCH.get();
        scratch.checked.clear();
        scratch.queue.clear();
        return walk(level, start, startState, actor, scratch.checked, scratch.queue, null);
    }

    private int walk(Level level, BlockPos start, BlockState startState, @Nullable Entity actor,
                     LongOpenHashSet checked, LongArrayFIFOQueue queue, @Nullable List<BlockPos> out) {
        int[][] neighbors = diagonal ? (downward ? ALL : LEVEL_AND_UP) : (downward ? FACES : FACES_NOT_DOWN);
        Block startBlock = startState.getBlock();
        boolean needNeighbor = neighborCondition.isPresent();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        long origin = start.asLong();
        checked.add(origin);
        queue.enqueue(origin);
        int size = 0;
        while (!queue.isEmpty()) {
            long current = queue.dequeueLong();
            int x = BlockPos.getX(current);
            int y = BlockPos.getY(current);
            int z = BlockPos.getZ(current);
            for (int[] offset : neighbors) {
                cursor.set(x + offset[0], y + offset[1], z + offset[2]);
                long key = cursor.asLong();
                if (!checked.add(key) || !level.isLoaded(cursor)) continue;
                BlockState state = level.getBlockState(cursor);
                boolean member = blockCondition.isPresent()
                    ? blockCondition.get().test(new BlockCtx(cursor.immutable(), state, level, actor))
                    : state.is(startBlock);
                if (member) {
                    size++;
                    if (out != null) out.add(cursor.immutable());
                    if (size >= limit) return needNeighbor ? 0 : size;
                    queue.enqueue(key);
                } else if (needNeighbor
                        && neighborCondition.get().test(new BlockCtx(cursor.immutable(), state, level, actor))) {
                    needNeighbor = false;
                }
            }
        }
        return needNeighbor ? 0 : size;
    }
}
