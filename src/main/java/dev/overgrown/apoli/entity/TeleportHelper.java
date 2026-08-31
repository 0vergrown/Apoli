package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class TeleportHelper {

    private TeleportHelper() {}

    public static @Nullable ServerLevel level(Entity entity, Optional<ResourceLocation> dimension) {
        if (!(entity.level() instanceof ServerLevel current)) return null;
        if (dimension.isEmpty()) return current;
        MinecraftServer server = current.getServer();
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension.get()));
    }

    public static boolean chunkLoaded(ServerLevel level, double x, double z) {
        return level.hasChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4);
    }

    public static boolean landingAllowed(Entity entity, ServerLevel level, double x, double y, double z,
                                         Optional<BlockCondition> blockCondition,
                                         Optional<EntityCondition> entityCondition) {
        if (blockCondition.isPresent()) {
            BlockPos below = BlockPos.containing(x, y - 1.0, z);
            if (!blockCondition.get().test(new BlockCtx(below, level.getBlockState(below), level))) return false;
        }
        if (entityCondition.isEmpty()) return true;
        if (entity.level() != level) return true;
        Vec3 origin = entity.position();
        entity.setPos(x, y, z);
        boolean allowed;
        try {
            allowed = entityCondition.get().test(new EntityCtx(entity, level));
        } finally {
            entity.setPos(origin.x, origin.y, origin.z);
        }
        return allowed;
    }

    public static @Nullable Entity teleport(Entity entity, ServerLevel level, double x, double y, double z,
                                            float yaw, float pitch) {
        Level origin = entity.level();
        if (!entity.teleportTo(level, x, y, z, Set.of(), yaw, pitch)) return null;
        if (origin == level || !entity.isRemoved()) return entity;
        return level.getEntity(entity.getUUID());
    }

    public static @Nullable Entity teleport(Entity entity, ServerLevel level, double x, double y, double z) {
        return teleport(entity, level, x, y, z, entity.getYRot(), entity.getXRot());
    }
}
