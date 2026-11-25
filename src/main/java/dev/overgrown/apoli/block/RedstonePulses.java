package dev.overgrown.apoli.block;

import dev.overgrown.apoli.Apoli;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RedstonePulses {

    public static final int ALL_SIDES = 0b111111;

    private RedstonePulses() {}

    public enum Mode implements StringRepresentable {
        RECEIVE("receive"),
        EMIT("emit");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final class Pulse {
        int strength;
        int ticks;
        int sides;
        boolean strong;

        Pulse(int strength, int ticks, int sides, boolean strong) {
            this.strength = strength;
            this.ticks = ticks;
            this.sides = sides;
            this.strong = strong;
        }

        boolean side(Direction direction) {
            return (sides & (1 << direction.get3DDataValue())) != 0;
        }
    }

    private static final class Dimension {
        final Long2ObjectOpenHashMap<Pulse> emit = new Long2ObjectOpenHashMap<>();
        final Long2ObjectOpenHashMap<Pulse> receive = new Long2ObjectOpenHashMap<>();

        int size() {
            return emit.size() + receive.size();
        }
    }

    private record Ended(ServerLevel level, long pos, Mode mode, boolean strong) {}

    private static final Map<ResourceKey<Level>, Dimension> DIMENSIONS = new HashMap<>();
    private static int active;

    public static void pulse(ServerLevel level, BlockPos pos, Mode mode, int strength, int ticks, int sides, boolean strong) {
        strength = Math.max(1, Math.min(15, strength));
        ticks = Math.max(1, ticks);
        sides &= ALL_SIDES;
        if (sides == 0) return;
        Dimension dimension = DIMENSIONS.computeIfAbsent(level.dimension(), d -> new Dimension());
        Long2ObjectOpenHashMap<Pulse> map = mode == Mode.EMIT ? dimension.emit : dimension.receive;
        long key = pos.asLong();
        Pulse pulse = map.get(key);
        if (pulse == null) {
            map.put(key, new Pulse(strength, ticks, sides, strong));
            active++;
        } else {
            pulse.ticks = Math.max(pulse.ticks, ticks);
            boolean stronger = strength > pulse.strength || (sides | pulse.sides) != pulse.sides || strong && !pulse.strong;
            if (!stronger) return;
            pulse.strength = Math.max(pulse.strength, strength);
            pulse.sides |= sides;
            pulse.strong |= strong;
            strong = pulse.strong;
        }
        notify(level, pos.immutable(), mode, strong);
    }

    public static void tick(MinecraftServer server) {
        if (active == 0) return;
        List<Ended> ended = null;
        var iterator = DIMENSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, Dimension> entry = iterator.next();
            ServerLevel level = server.getLevel(entry.getKey());
            Dimension dimension = entry.getValue();
            if (level == null) {
                active -= dimension.size();
                iterator.remove();
                continue;
            }
            ended = expire(level, dimension.emit, Mode.EMIT, ended);
            ended = expire(level, dimension.receive, Mode.RECEIVE, ended);
            if (dimension.size() == 0) iterator.remove();
        }
        if (ended == null) return;
        for (Ended e : ended) notify(e.level(), BlockPos.of(e.pos()), e.mode(), e.strong());
    }

    private static List<Ended> expire(ServerLevel level, Long2ObjectOpenHashMap<Pulse> map, Mode mode, List<Ended> ended) {
        if (map.isEmpty()) return ended;
        var iterator = map.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Long2ObjectMap.Entry<Pulse> entry = iterator.next();
            Pulse pulse = entry.getValue();
            if (--pulse.ticks > 0) continue;
            if (ended == null) ended = new ArrayList<>();
            ended.add(new Ended(level, entry.getLongKey(), mode, pulse.strong));
            iterator.remove();
            active--;
        }
        return ended;
    }

    private static void notify(ServerLevel level, BlockPos pos, Mode mode, boolean strong) {
        if (!level.isLoaded(pos)) return;
        try {
            Block source = Blocks.REDSTONE_BLOCK;
            if (mode == Mode.EMIT) {
                level.updateNeighborsAt(pos, source);
                if (strong) for (Direction direction : Direction.values()) level.updateNeighborsAt(pos.relative(direction), source);
            } else {
                level.neighborChanged(pos, source, pos.above());
                if (strong) level.updateNeighborsAt(pos, source);
            }
        } catch (RuntimeException e) {
            Apoli.LOGGER.warn("[Apoli] Redstone pulse at {} failed to update its neighbours: {}", pos, e.toString());
        }
    }

    public static int signal(SignalGetter getter, BlockPos pos, Direction direction, int original, boolean direct) {
        if (active == 0 || original >= 15 || !(getter instanceof ServerLevel level)) return original;
        Dimension dimension = DIMENSIONS.get(level.dimension());
        if (dimension == null) return original;
        int best = original;
        long at = pos.asLong();
        if (!dimension.emit.isEmpty()) {
            Pulse emit = dimension.emit.get(at);
            if (emit != null && (!direct || emit.strong) && emit.side(direction.getOpposite())) best = Math.max(best, emit.strength);
        }
        if (!dimension.receive.isEmpty()) {
            Pulse receive = dimension.receive.get(BlockPos.offset(at, direction.getOpposite()));
            if (receive != null && (!direct || receive.strong) && receive.side(direction)) best = Math.max(best, receive.strength);
        }
        return best;
    }

    public static void clear() {
        DIMENSIONS.clear();
        active = 0;
    }
}
