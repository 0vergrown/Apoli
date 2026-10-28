package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Pair;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.BiomeCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BiomeCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ModifyPlayerSpawnHandler {
    private ModifyPlayerSpawnHandler() {}

    private static final int BIOME_SEARCH_RADIUS = 6400;
    private static final int BIOME_HORIZONTAL_STEP = 32;
    private static final int BIOME_VERTICAL_STEP = 64;
    private static final int STRUCTURE_SEARCH_RADIUS = 100;
    private static final long FAILED_SCAN_RETRY_TICKS = 6000L;

    private static final Map<ModifyPlayerSpawnPower.Config, Search> SEARCHES = new IdentityHashMap<>();

    private record Spawn(ServerLevel level, Vec3 position) {
        BlockPos blockPos() {
            return BlockPos.containing(position);
        }
    }

    private static final class Search {
        private final ServerLevel level;
        private final BlockPos origin;
        private final @Nullable BlockPos start;
        private final @Nullable String missing;
        private long retryScanAt;

        private Search(ServerLevel level, BlockPos origin, @Nullable BlockPos start, @Nullable String missing) {
            this.level = level;
            this.origin = origin;
            this.start = start;
            this.missing = missing;
        }
    }

    public static void clearCache() {
        SEARCHES.clear();
    }

    public static @Nullable ModifyPlayerSpawnPower.Config firstActive(ServerPlayer player) {
        return PowerLookup.firstActive(player, ApoliIds.MODIFY_PLAYER_SPAWN, ModifyPlayerSpawnPower.Config.class);
    }

    public static boolean anyOtherActive(PowerContainer holder, ResourceLocation ignored) {
        List<ResourceLocation> powers = holder.powersOfType(ApoliIds.MODIFY_PLAYER_SPAWN);
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation id = powers.get(i);
            if (!id.equals(ignored) && !holder.isSuppressed(id)) return true;
        }
        return false;
    }

    public static @Nullable BlockPos respawnPosition(ServerPlayer player, @Nullable BlockPos original) {
        ModifyPlayerSpawnPower.Config cfg = firstActive(player);
        if (cfg == null) return original;
        if (original != null && !isObstructed(player, original)) return original;

        Spawn spawn = resolve(player, cfg);
        if (spawn == null) return original;

        BlockPos pos = spawn.blockPos();
        player.setRespawnPosition(spawn.level().dimension(), pos, player.getYRot(), true, false);
        return pos;
    }

    private static boolean isObstructed(ServerPlayer player, BlockPos original) {
        ServerLevel level = player.server.getLevel(player.getRespawnDimension());
        if (level == null) return true;
        return Player.findRespawnPositionAndUseSpawnBlock(
            level, original, player.getRespawnAngle(), player.isRespawnForced(), true).isEmpty();
    }

    public static void teleportToModifiedSpawn(ServerPlayer player) {
        ModifyPlayerSpawnPower.Config cfg = firstActive(player);
        if (cfg == null) return;
        if (player.getRespawnPosition() != null && dimension(cfg).equals(player.getRespawnDimension())) return;

        Spawn spawn = resolve(player, cfg);
        if (spawn == null) return;

        Vec3 at = spawn.position();
        player.setRespawnPosition(spawn.level().dimension(), spawn.blockPos(), player.getYRot(), true, false);
        player.teleportTo(spawn.level(), at.x, at.y, at.z, player.getYRot(), player.getXRot());
    }

    private static ResourceKey<Level> dimension(ModifyPlayerSpawnPower.Config cfg) {
        return ResourceKey.create(Registries.DIMENSION, cfg.dimension());
    }

    private static @Nullable Spawn resolve(ServerPlayer player, ModifyPlayerSpawnPower.Config cfg) {
        ServerLevel target = player.server.getLevel(dimension(cfg));
        if (target == null) {
            Apoli.LOGGER.warn("[Apoli] apoli:modify_player_spawn refers to unknown dimension '{}'.", cfg.dimension());
            return null;
        }

        int center = target.dimensionType().logicalHeight() / 2;
        BlockPos origin = cfg.spawnStrategy().apply(
            player.serverLevel().getSharedSpawnPos(), center, cfg.dimensionDistanceMultiplier().orElse(1.0F));

        Search search = search(target, cfg, origin);
        if (search.start == null) {
            complain(player, cfg, search.missing);
            return null;
        }

        long now = target.getGameTime();
        if (now < search.retryScanAt) return null;

        Vec3 spawn = findValidSpawn(target, search.start, center, cfg);
        if (spawn == null) {
            search.retryScanAt = now + FAILED_SCAN_RETRY_TICKS;
            Apoli.LOGGER.warn("[Apoli] apoli:modify_player_spawn could not find a safe spot for {} within {} blocks of {} in '{}'.",
                player.getName().getString(), cfg.searchRadius(), search.start.toShortString(), cfg.dimension());
            return null;
        }
        return new Spawn(target, spawn);
    }

    private static Search search(ServerLevel target, ModifyPlayerSpawnPower.Config cfg, BlockPos origin) {
        Search cached = SEARCHES.get(cfg);
        if (cached != null && cached.level == target && cached.origin.equals(origin)) return cached;
        Search fresh = locate(target, cfg, origin);
        SEARCHES.put(cfg, fresh);
        return fresh;
    }

    private static Search locate(ServerLevel target, ModifyPlayerSpawnPower.Config cfg, BlockPos origin) {
        BlockPos start = origin;

        if (cfg.biome().isPresent() || cfg.biomeCondition().isPresent()) {
            Pair<BlockPos, Holder<Biome>> biome = target.findClosestBiome3d(biomes(target, cfg, origin),
                origin, BIOME_SEARCH_RADIUS, BIOME_HORIZONTAL_STEP, BIOME_VERTICAL_STEP);
            if (biome == null) return new Search(target, origin, null, biomeName(cfg));
            start = biome.getFirst();
        }

        if (cfg.structure().isPresent() || cfg.structureTag().isPresent()) {
            Pair<BlockPos, Holder<Structure>> structure = target.getChunkSource().getGenerator()
                .findNearestMapStructure(target, structures(target, cfg), start, STRUCTURE_SEARCH_RADIUS, false);
            if (structure == null) return new Search(target, origin, null, structureName(cfg));
            start = structure.getFirst();
        }

        return new Search(target, origin, start, null);
    }

    private static Predicate<Holder<Biome>> biomes(ServerLevel target, ModifyPlayerSpawnPower.Config cfg, BlockPos origin) {
        ResourceKey<Biome> key = cfg.biome().map(id -> ResourceKey.create(Registries.BIOME, id)).orElse(null);
        BiomeCondition condition = cfg.biomeCondition().orElse(null);
        return holder -> (key != null && holder.is(key))
            || (condition != null && condition.test(new BiomeCtx(holder, origin, target)));
    }

    private static HolderSet<Structure> structures(ServerLevel target, ModifyPlayerSpawnPower.Config cfg) {
        Registry<Structure> registry = target.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> holders = new ArrayList<>();
        cfg.structure()
            .flatMap(id -> registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id)))
            .ifPresent(holders::add);
        cfg.structureTag().flatMap(registry::getTag).ifPresent(tag -> {
            for (Holder<Structure> holder : tag) {
                if (!holders.contains(holder)) holders.add(holder);
            }
        });
        return HolderSet.direct(holders);
    }

    private static String biomeName(ModifyPlayerSpawnPower.Config cfg) {
        return either(
            cfg.biome().map(id -> "biome \"" + id + "\"").orElse(null),
            cfg.biomeCondition().isPresent() ? "a biome matching its biome_condition" : null);
    }

    private static String structureName(ModifyPlayerSpawnPower.Config cfg) {
        return either(
            cfg.structure().map(id -> "structure \"" + id + "\"").orElse(null),
            cfg.structureTag().map(tag -> "a structure in \"#" + tag.location() + "\"").orElse(null));
    }

    private static String either(@Nullable String first, @Nullable String second) {
        if (first == null) return second;
        return second == null ? first : first + " or " + second;
    }

    private static void complain(ServerPlayer player, ModifyPlayerSpawnPower.Config cfg, String what) {
        Apoli.LOGGER.warn("[Apoli] apoli:modify_player_spawn could not set {}'s spawn at {}: none found in '{}'.",
            player.getName().getString(), what, cfg.dimension());
        player.sendSystemMessage(Component
            .literal("Couldn't set your spawn point at %s — none could be found in \"%s\".".formatted(what, cfg.dimension()))
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }

    private static @Nullable Vec3 findValidSpawn(ServerLevel level, BlockPos start, int preferredY, ModifyPlayerSpawnPower.Config cfg) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = Math.min(level.getMaxBuildHeight(),
            level.getMinBuildHeight() + level.dimensionType().logicalHeight()) - 2;
        int startY = Mth.clamp(preferredY, minY, maxY);
        BlockCondition condition = cfg.blockCondition().orElse(null);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (BlockPos column : BlockPos.spiralAround(start, cfg.searchRadius(), Direction.EAST, Direction.SOUTH)) {
            Vec3 found = findInColumn(level, cursor, column.getX(), column.getZ(), startY, minY, maxY, condition);
            if (found != null) return found;
        }
        return null;
    }

    private static @Nullable Vec3 findInColumn(ServerLevel level, BlockPos.MutableBlockPos cursor, int x, int z,
                                               int startY, int minY, int maxY, @Nullable BlockCondition condition) {
        for (int offset = 0; offset <= maxY - minY; offset++) {
            int down = startY - offset;
            int up = startY + offset;
            if (down < minY && up > maxY) break;

            if (down >= minY) {
                Vec3 found = safeSpot(level, cursor.set(x, down, z), condition);
                if (found != null) return found;
            }
            if (offset > 0 && up <= maxY) {
                Vec3 found = safeSpot(level, cursor.set(x, up, z), condition);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static @Nullable Vec3 safeSpot(ServerLevel level, BlockPos.MutableBlockPos cursor, @Nullable BlockCondition condition) {
        Vec3 found = DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, cursor, true);
        if (found == null || condition == null) return found;
        BlockPos pos = cursor.immutable();
        return condition.test(new BlockCtx(pos, level.getBlockState(pos), level)) ? found : null;
    }
}
