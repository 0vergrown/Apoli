package dev.overgrown.apoli.client.glow;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.client.ClientPowerState;
import dev.overgrown.apoli.client.ClientResolvedPowers;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.BlockGlowStyle;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.builtin.BlockGlowPower;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class BlockGlowScanner {

    private static final int BLOCK_BUDGET = 16384;
    private static final int REFRESH_TICKS = 100;
    private static final long[] NO_HITS = new long[0];

    public interface HitVisitor {
        void visit(long pos, BlockGlowStyle style);
    }

    private static final class Section {
        final long[] hits;
        final long scannedAt;

        Section(long[] hits, long scannedAt) {
            this.hits = hits;
            this.scannedAt = scannedAt;
        }
    }

    private static final class Scan {
        final ResourceLocation powerId;
        final BlockGlowPower.Config config;
        final Long2ObjectOpenHashMap<Section> sections = new Long2ObjectOpenHashMap<>();
        boolean broken;

        Scan(ResourceLocation powerId, BlockGlowPower.Config config) {
            this.powerId = powerId;
            this.config = config;
        }
    }

    private static final List<Scan> SCANS = new ArrayList<>();
    private static final Set<ResourceLocation> WARNED = new HashSet<>();
    private static final LongArrayList SCRATCH = new LongArrayList();
    private static int[][] offsets = new int[0][];
    private static int offsetRadius = -1;
    private static @Nullable ClientLevel level;
    private static long ticks;

    private BlockGlowScanner() {}

    public static void clientTick(Minecraft mc) {
        ticks++;
        LocalPlayer player = mc.player;
        ClientLevel current = mc.level;
        if (player == null || current == null) {
            if (!SCANS.isEmpty()) SCANS.clear();
            level = null;
            return;
        }
        if (current != level) {
            SCANS.clear();
            level = current;
        }
        refresh();
        if (SCANS.isEmpty()) return;
        int budget = BLOCK_BUDGET;
        for (int i = 0, n = SCANS.size(); i < n && budget > 0; i++) {
            budget = step(SCANS.get(i), current, player, budget);
        }
    }

    private static void refresh() {
        Set<ResourceLocation> local = ClientPowerState.localPowers();
        if (local.isEmpty()) {
            SCANS.clear();
            return;
        }
        SCANS.removeIf(scan -> !active(scan.powerId, scan.config));
        for (ResourceLocation powerId : local) {
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof BlockGlowPower.Config config)) continue;
            if (!ClientResolvedPowers.isActive(powerId)) continue;
            boolean known = false;
            for (int i = 0, n = SCANS.size(); i < n; i++) {
                if (SCANS.get(i).powerId.equals(powerId)) {
                    known = true;
                    break;
                }
            }
            if (!known) SCANS.add(new Scan(powerId, config));
        }
    }

    private static boolean active(ResourceLocation powerId, BlockGlowPower.Config config) {
        if (!ClientResolvedPowers.isActive(powerId)) return false;
        if (!ClientPowerState.localPowers().contains(powerId)) return false;
        Power power = ApoliPowers.get(powerId);
        return power != null && power.config() == config;
    }

    private static int step(Scan scan, ClientLevel level, LocalPlayer player, int budget) {
        if (scan.broken) return budget;
        double range = scan.config.range();
        int reach = (int) Math.ceil(range);
        int sectionRadius = (reach >> 4) + 1;
        int originX = SectionPos.blockToSectionCoord(player.getBlockX());
        int originY = SectionPos.blockToSectionCoord(player.getBlockY());
        int originZ = SectionPos.blockToSectionCoord(player.getBlockZ());
        if ((ticks & 31) == 0) {
            var keys = scan.sections.keySet().iterator();
            while (keys.hasNext()) {
                long key = keys.nextLong();
                if (Math.abs(SectionPos.x(key) - originX) > sectionRadius + 1
                    || Math.abs(SectionPos.y(key) - originY) > sectionRadius + 1
                    || Math.abs(SectionPos.z(key) - originZ) > sectionRadius + 1) {
                    keys.remove();
                }
            }
        }
        int minSection = level.getMinSection();
        int maxSection = level.getMaxSection();
        int[][] order = offsets(sectionRadius);
        double rangeSq = (range + 14.0) * (range + 14.0);
        double eyeX = player.getX();
        double eyeY = player.getEyeY();
        double eyeZ = player.getZ();
        for (int[] offset : order) {
            int sx = originX + offset[0];
            int sy = originY + offset[1];
            int sz = originZ + offset[2];
            if (sy < minSection || sy >= maxSection) continue;
            double cx = (sx << 4) + 8.0 - eyeX;
            double cy = (sy << 4) + 8.0 - eyeY;
            double cz = (sz << 4) + 8.0 - eyeZ;
            if (cx * cx + cy * cy + cz * cz > rangeSq) continue;
            long key = SectionPos.asLong(sx, sy, sz);
            Section known = scan.sections.get(key);
            if (known != null && ticks - known.scannedAt < REFRESH_TICKS) continue;
            if (budget <= 0) return 0;
            ChunkAccess chunk = level.getChunk(sx, sz, ChunkStatus.FULL, false);
            if (chunk == null) continue;
            long[] hits;
            try {
                hits = scanSection(scan.config.blockCondition(), level, player, chunk, sx, sy, sz);
            } catch (RuntimeException e) {
                scan.broken = true;
                if (WARNED.add(scan.powerId)) {
                    Apoli.LOGGER.warn("[Apoli] {} stopped highlighting blocks because its block_condition failed on the "
                        + "client: {}", scan.powerId, e.toString());
                }
                return budget;
            }
            scan.sections.put(key, new Section(hits, ticks));
            budget -= 4096;
        }
        return budget;
    }

    private static long[] scanSection(BlockCondition condition, ClientLevel level, LocalPlayer player, ChunkAccess chunk,
                                      int sx, int sy, int sz) {
        int index = level.getSectionIndexFromSectionY(sy);
        LevelChunkSection[] sections = chunk.getSections();
        if (index < 0 || index >= sections.length) return NO_HITS;
        LevelChunkSection section = sections[index];
        if (section == null || section.hasOnlyAir()) return NO_HITS;
        int baseX = sx << 4;
        int baseY = sy << 4;
        int baseZ = sz << 4;
        SCRATCH.clear();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = section.getBlockState(x, y, z);
                    if (state.isAir()) continue;
                    BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);
                    if (condition.test(new BlockCtx(pos, state, level, player, null))) SCRATCH.add(pos.asLong());
                }
            }
        }
        return SCRATCH.isEmpty() ? NO_HITS : SCRATCH.toLongArray();
    }

    private static int[][] offsets(int radius) {
        if (radius == offsetRadius) return offsets;
        List<int[]> all = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) all.add(new int[] {x, y, z});
            }
        }
        int[][] sorted = all.toArray(new int[0][]);
        Arrays.sort(sorted, (a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1] + a[2] * a[2], b[0] * b[0] + b[1] * b[1] + b[2] * b[2]));
        offsets = sorted;
        offsetRadius = radius;
        return sorted;
    }

    public static boolean hasHits() {
        return !SCANS.isEmpty();
    }

    public static void forEachHit(LocalPlayer player, HitVisitor visitor) {
        double eyeX = player.getX();
        double eyeY = player.getEyeY();
        double eyeZ = player.getZ();
        for (int s = 0, n = SCANS.size(); s < n; s++) {
            Scan scan = SCANS.get(s);
            double range = scan.config.range();
            double rangeSq = range * range;
            BlockGlowStyle style = scan.config.style();
            for (Section section : scan.sections.values()) {
                long[] hits = section.hits;
                for (int i = 0; i < hits.length; i++) {
                    long pos = hits[i];
                    double dx = BlockPos.getX(pos) + 0.5 - eyeX;
                    double dy = BlockPos.getY(pos) + 0.5 - eyeY;
                    double dz = BlockPos.getZ(pos) + 0.5 - eyeZ;
                    if (dx * dx + dy * dy + dz * dz > rangeSq) continue;
                    visitor.visit(pos, style);
                }
            }
        }
    }

    public static void blockChanged(BlockPos pos) {
        if (SCANS.isEmpty()) return;
        long key = SectionPos.asLong(SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()));
        for (int i = 0, n = SCANS.size(); i < n; i++) SCANS.get(i).sections.remove(key);
    }

    public static void chunkLoaded(int chunkX, int chunkZ) {
        if (SCANS.isEmpty() || level == null) return;
        int min = level.getMinSection();
        int max = level.getMaxSection();
        for (int i = 0, n = SCANS.size(); i < n; i++) {
            Long2ObjectOpenHashMap<Section> sections = SCANS.get(i).sections;
            if (sections.isEmpty()) continue;
            for (int sy = min; sy < max; sy++) sections.remove(SectionPos.asLong(chunkX, sy, chunkZ));
        }
    }

    public static void clear() {
        SCANS.clear();
        level = null;
    }
}
