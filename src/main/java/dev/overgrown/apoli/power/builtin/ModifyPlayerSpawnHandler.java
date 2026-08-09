package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Pair;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ModifyPlayerSpawnHandler {
    private ModifyPlayerSpawnHandler() {}

    private static final int SEARCH_RANGE = 64;
    private static final int BIOME_SEARCH_RADIUS = 6400;
    private static final int BIOME_HORIZONTAL_STEP = 32;
    private static final int BIOME_VERTICAL_STEP = 64;
    private static final int STRUCTURE_SEARCH_RADIUS = 100;
    private static final int SPAWN_CHUNK_TICKET_RADIUS = 11;

    public static @Nullable ModifyPlayerSpawnPower.Config firstActive(ServerPlayer player) {
        PowerContainer container = PowerContainer.of(player);
        if (container == null || container.isEmpty()) return null;
        if (container.powersOfType(ApoliIds.MODIFY_PLAYER_SPAWN).isEmpty()) return null;

        ModifyPlayerSpawnPower.Config[] found = new ModifyPlayerSpawnPower.Config[1];
        PowerLookup.forEach(player, ApoliIds.MODIFY_PLAYER_SPAWN, ModifyPlayerSpawnPower.Config.class, cfg -> {
            if (found[0] == null) found[0] = cfg;
        });
        return found[0];
    }

    public static boolean anyOtherActive(PowerContainer holder, ResourceLocation ignored) {
        for (ResourceLocation id : holder.allPowers()) {
            if (id.equals(ignored) || holder.isSuppressed(id)) continue;
            Power power = ApoliPowers.get(id);
            if (power == null) continue;
            if (!ApoliIds.MODIFY_PLAYER_SPAWN.equals(PowerTypeRegistry.resolveId(power.typeId()))) continue;
            return true;
        }
        return false;
    }

    public static @Nullable BlockPos respawnPosition(ServerPlayer player, @Nullable BlockPos original) {
        ModifyPlayerSpawnPower.Config cfg = firstActive(player);
        if (cfg == null) return original;
        if (original != null && !isObstructed(player, original)) return original;

        Pair<ServerLevel, BlockPos> spawn = resolve(player, cfg);
        if (spawn == null) return original;

        ServerLevel level = spawn.getFirst();
        BlockPos pos = spawn.getSecond();
        player.setRespawnPosition(level.dimension(), pos, player.getYRot(), true, false);
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
        if (player.getRespawnPosition() != null
            && ResourceKey.create(Registries.DIMENSION, cfg.dimension()).equals(player.getRespawnDimension())) {
            return;
        }

        Pair<ServerLevel, BlockPos> spawn = resolve(player, cfg);
        if (spawn == null) return;

        ServerLevel level = spawn.getFirst();
        BlockPos pos = spawn.getSecond();
        player.setRespawnPosition(level.dimension(), pos, player.getYRot(), true, false);
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
            player.getYRot(), player.getXRot());
    }

    private static @Nullable Pair<ServerLevel, BlockPos> resolve(ServerPlayer player, ModifyPlayerSpawnPower.Config cfg) {
        ServerLevel target = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, cfg.dimension()));
        if (target == null) {
            Apoli.LOGGER.warn("[Apoli] apoli:modify_player_spawn refers to unknown dimension '{}'.", cfg.dimension());
            return null;
        }

        int center = target.dimensionType().logicalHeight() / 2;
        BlockPos origin = cfg.spawnStrategy().apply(
            player.serverLevel().getSharedSpawnPos(), center, cfg.dimensionDistanceMultiplier().orElse(1.0F));

        BlockPos biomeOrigin = locateBiome(player, target, cfg, origin);
        if (biomeOrigin == null) return null;

        BlockPos searchStart = locateStructure(player, target, cfg, biomeOrigin);
        if (searchStart == null) return null;

        Vec3 spawn = findValidSpawn(target, searchStart, center);
        if (spawn == null) {
            Apoli.LOGGER.warn("[Apoli] apoli:modify_player_spawn could not find a safe spot for {} in '{}'.",
                player.getName().getString(), cfg.dimension());
            return null;
        }

        BlockPos pos = BlockPos.containing(spawn);
        target.getChunkSource().addRegionTicket(TicketType.START, new ChunkPos(pos), SPAWN_CHUNK_TICKET_RADIUS, Unit.INSTANCE);
        return Pair.of(target, pos);
    }

    private static @Nullable BlockPos locateBiome(ServerPlayer player, ServerLevel target,
                                                  ModifyPlayerSpawnPower.Config cfg, BlockPos origin) {
        if (cfg.biome().isEmpty()) return origin;

        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, cfg.biome().get());
        Pair<BlockPos, Holder<Biome>> found = target.findClosestBiome3d(
            holder -> holder.is(key), origin, BIOME_SEARCH_RADIUS, BIOME_HORIZONTAL_STEP, BIOME_VERTICAL_STEP);
        if (found != null) return found.getFirst();

        complain(player, cfg, "biome \"" + cfg.biome().get() + "\"");
        return null;
    }

    private static @Nullable BlockPos locateStructure(ServerPlayer player, ServerLevel target,
                                                      ModifyPlayerSpawnPower.Config cfg, BlockPos origin) {
        if (cfg.structure().isEmpty()) return origin;

        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, cfg.structure().get());
        Holder.Reference<Structure> holder = target.registryAccess()
            .registryOrThrow(Registries.STRUCTURE).getHolder(key).orElse(null);
        if (holder == null) {
            complain(player, cfg, "structure \"" + cfg.structure().get() + "\"");
            return null;
        }

        Pair<BlockPos, Holder<Structure>> found = target.getChunkSource().getGenerator()
            .findNearestMapStructure(target, HolderSet.direct(holder), origin, STRUCTURE_SEARCH_RADIUS, false);
        if (found != null) return found.getFirst();

        complain(player, cfg, "structure \"" + cfg.structure().get() + "\"");
        return null;
    }

    private static void complain(ServerPlayer player, ModifyPlayerSpawnPower.Config cfg, String what) {
        Apoli.LOGGER.warn("[Apoli] apoli:modify_player_spawn could not set {}'s spawn at {}: none found in '{}'.",
            player.getName().getString(), what, cfg.dimension());
        player.sendSystemMessage(Component
            .literal("Couldn't set your spawn point at %s — none could be found in \"%s\".".formatted(what, cfg.dimension()))
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }

    private static @Nullable Vec3 findValidSpawn(ServerLevel level, BlockPos start, int preferredY) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        int startY = Mth.clamp(preferredY, minY, maxY);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int x = start.getX();
        int z = start.getZ();
        int dx = 1;
        int dz = 0;
        int segment = 1;
        int passed = 0;

        for (int step = 0; step < SEARCH_RANGE * SEARCH_RANGE; step++) {
            Vec3 found = findInColumn(level, cursor, x, z, startY, minY, maxY);
            if (found != null) return found;

            x += dx;
            z += dz;
            if (++passed == segment) {
                passed = 0;
                int turn = dx;
                dx = -dz;
                dz = turn;
                if (dz == 0) segment++;
            }
            if (segment > SEARCH_RANGE) break;
        }
        return null;
    }

    private static @Nullable Vec3 findInColumn(ServerLevel level, BlockPos.MutableBlockPos cursor,
                                               int x, int z, int startY, int minY, int maxY) {
        for (int offset = 0; offset <= maxY - minY; offset++) {
            int down = startY - offset;
            int up = startY + offset;
            if (down < minY && up > maxY) break;

            if (down >= minY) {
                cursor.set(x, down, z);
                Vec3 found = DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, cursor, true);
                if (found != null) return found;
            }
            if (offset > 0 && up <= maxY) {
                cursor.set(x, up, z);
                Vec3 found = DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, cursor, true);
                if (found != null) return found;
            }
        }
        return null;
    }
}
