package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Heightmap;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class RandomTeleportAction implements ActionType<EntityCtx, RandomTeleportAction.Cfg> {
    public record Cfg(
        float areaWidth,
        float areaHeight,
        Optional<Heightmap> heightmap,
        Optional<Integer> attempts,
        Optional<BlockCondition> landingBlockCondition,
        Optional<EntityCondition> landingCondition,
        Vector landingOffset,
        boolean loadedChunksOnly,
        Optional<EntityAction> successAction,
        Optional<EntityAction> failAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("area_width", 8.0f).forGetter(Cfg::areaWidth),
            Codec.FLOAT.optionalFieldOf("area_height", 8.0f).forGetter(Cfg::areaHeight),
            Heightmap.CODEC.optionalFieldOf("heightmap").forGetter(Cfg::heightmap),
            Codec.INT.optionalFieldOf("attempts").forGetter(Cfg::attempts),
            BlockCondition.CODEC.optionalFieldOf("landing_block_condition").forGetter(Cfg::landingBlockCondition),
            EntityCondition.CODEC.optionalFieldOf("landing_condition").forGetter(Cfg::landingCondition),
            Vector.CODEC.optionalFieldOf("landing_offset", Vector.ZERO).forGetter(Cfg::landingOffset),
            Codec.BOOL.optionalFieldOf("loaded_chunks_only", true).forGetter(Cfg::loadedChunksOnly),
            EntityAction.CODEC.optionalFieldOf("success_action").forGetter(Cfg::successAction),
            EntityAction.CODEC.optionalFieldOf("fail_action").forGetter(Cfg::failAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.entity();
        Level level = ctx.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        RandomSource random = entity.getRandom();
        int attempts = cfg.attempts.orElse((int) (cfg.areaWidth * 2 + cfg.areaHeight * 2));
        int w = (int) cfg.areaWidth;
        int h = (int) cfg.areaHeight;
        BlockPos origin = entity.blockPosition();
        for (int i = 0; i < attempts; i++) {
            int dx = random.nextInt(w * 2 + 1) - w;
            int dz = random.nextInt(w * 2 + 1) - w;
            int dy = random.nextInt(h * 2 + 1) - h;
            BlockPos candidate = origin.offset(dx, dy, dz);
            if (cfg.loadedChunksOnly && !serverLevel.hasChunk(candidate.getX() >> 4, candidate.getZ() >> 4)) continue;
            if (cfg.heightmap.isPresent()) {
                int top = serverLevel.getHeight(cfg.heightmap.get().vanilla(), candidate.getX(), candidate.getZ());
                candidate = new BlockPos(candidate.getX(), top, candidate.getZ());
            }
            if (!isValidLanding(candidate, cfg, entity, serverLevel)) continue;
            double x = candidate.getX() + cfg.landingOffset.x();
            double y = candidate.getY() + cfg.landingOffset.y();
            double z = candidate.getZ() + cfg.landingOffset.z();
            entity.teleportTo(x, y, z);
            cfg.successAction.ifPresent(a -> a.run(ctx));
            return;
        }
        cfg.failAction.ifPresent(a -> a.run(ctx));
    }

    private static boolean isValidLanding(BlockPos pos, Cfg cfg, LivingEntity entity, ServerLevel level) {
        if (cfg.landingBlockCondition.isPresent()) {
            BlockState state = level.getBlockState(pos.below());
            if (!cfg.landingBlockCondition.get().test(new BlockCtx(pos.below(), state, level))) return false;
        } else if (!level.getBlockState(pos.below()).blocksMotion()) {
            return false;
        }
        if (cfg.landingCondition.isPresent()) {
            return cfg.landingCondition.get().test(new EntityCtx(entity, level));
        }
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}
