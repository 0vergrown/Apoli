package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

public final class BlockCollisionCondition implements ConditionType<EntityCtx, BlockCollisionCondition.Cfg> {
    public record Cfg(Optional<BlockCondition> blockCondition, float offsetX, float offsetY, float offsetZ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Cfg::blockCondition),
            Codec.FLOAT.optionalFieldOf("offset_x", 0f).forGetter(Cfg::offsetX),
            Codec.FLOAT.optionalFieldOf("offset_y", 0f).forGetter(Cfg::offsetY),
            Codec.FLOAT.optionalFieldOf("offset_z", 0f).forGetter(Cfg::offsetZ)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        AABB box = ctx.entity().getBoundingBox();
        double w = box.getXsize(), h = box.getYsize(), d = box.getZsize();
        AABB shifted = box.move(cfg.offsetX * w, cfg.offsetY * h, cfg.offsetZ * d);
        if (cfg.blockCondition.isEmpty()) {
            return !ctx.level().noCollision(ctx.entity(), shifted);
        }
        int minX = (int) Math.floor(shifted.minX), maxX = (int) Math.floor(shifted.maxX);
        int minY = (int) Math.floor(shifted.minY), maxY = (int) Math.floor(shifted.maxY);
        int minZ = (int) Math.floor(shifted.minZ), maxZ = (int) Math.floor(shifted.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            pos.set(x, y, z);
            if (!cfg.blockCondition.get().test(new BlockCtx(pos.immutable(), ctx.level().getBlockState(pos), ctx.level()))) continue;
            if (!ctx.level().getBlockState(pos).getCollisionShape(ctx.level(), pos).isEmpty()) return true;
        }
        return false;
    }
}
