package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.TeleportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class TeleportToSpawnAction implements ActionType<EntityCtx, TeleportToSpawnAction.Cfg> {
    public record Cfg(
        boolean playerSpawn,
        Optional<EntityAction> successAction,
        Optional<EntityAction> failAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("player_spawn", true).forGetter(Cfg::playerSpawn),
            LoggedOptionalField.of("success_action", EntityAction.CODEC).forGetter(Cfg::successAction),
            LoggedOptionalField.of("fail_action", EntityAction.CODEC).forGetter(Cfg::failAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null || !(entity.level() instanceof ServerLevel current)) return;
        MinecraftServer server = current.getServer();

        ServerLevel level = null;
        Vec3 destination = null;
        float yaw = 0.0F;

        if (cfg.playerSpawn && entity instanceof ServerPlayer player) {
            BlockPos respawn = player.getRespawnPosition();
            ResourceKey<Level> dimension = player.getRespawnDimension();
            ServerLevel respawnLevel = respawn == null ? null : server.getLevel(dimension);
            if (respawnLevel != null) {
                float angle = player.getRespawnAngle();
                Optional<Vec3> found = Player.findRespawnPositionAndUseSpawnBlock(
                    respawnLevel, respawn, angle, player.isRespawnForced(), true);
                if (found.isPresent()) {
                    level = respawnLevel;
                    destination = found.get();
                    yaw = spawnYaw(respawnLevel, respawn, destination, angle);
                }
            }
        }
        if (level == null || destination == null) {
            level = server.overworld();
            BlockPos shared = level.getSharedSpawnPos();
            destination = new Vec3(shared.getX() + 0.5, shared.getY(), shared.getZ() + 0.5);
            yaw = level.getSharedSpawnAngle();
        }

        Entity moved = TeleportHelper.teleport(entity, level,
            destination.x, destination.y, destination.z, yaw, 0.0F);
        if (moved == null) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }
        cfg.successAction.ifPresent(a -> a.run(new EntityCtx(moved, moved.level())));
    }

    private static float spawnYaw(ServerLevel level, BlockPos anchor, Vec3 stand, float fallback) {
        BlockState state = level.getBlockState(anchor);
        if (!state.is(BlockTags.BEDS) && !state.is(Blocks.RESPAWN_ANCHOR)) return fallback;
        Vec3 away = Vec3.atBottomCenterOf(anchor).subtract(stand).normalize();
        return (float) Mth.wrapDegrees(Mth.atan2(away.z, away.x) * (180.0 / Math.PI) - 90.0);
    }
}
