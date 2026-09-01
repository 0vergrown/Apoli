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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

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
        BlockPos pos = null;
        float yaw = 0.0F;

        if (cfg.playerSpawn && entity instanceof ServerPlayer player) {
            BlockPos respawn = player.getRespawnPosition();
            ResourceKey<Level> dimension = player.getRespawnDimension();
            if (respawn != null) {
                ServerLevel respawnLevel = server.getLevel(dimension);
                if (respawnLevel != null) {
                    level = respawnLevel;
                    pos = respawn;
                    yaw = player.getRespawnAngle();
                }
            }
        }
        if (level == null) {
            level = server.overworld();
            pos = level.getSharedSpawnPos();
            yaw = level.getSharedSpawnAngle();
        }

        Entity moved = TeleportHelper.teleport(entity, level,
            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
        if (moved == null) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
            return;
        }
        cfg.successAction.ifPresent(a -> a.run(new EntityCtx(moved, moved.level())));
    }
}
