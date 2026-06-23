package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Nbt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class AddBlockAction implements ActionType<BlockCtx, AddBlockAction.Cfg> {
    public record Cfg(ResourceLocation block, Optional<Nbt> nbt) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("block").forGetter(Cfg::block),
            Nbt.CODEC.optionalFieldOf("nbt").forGetter(Cfg::nbt)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        if (!ctx.level().getBlockState(ctx.pos()).canBeReplaced()) return;
        Block block = BuiltInRegistries.BLOCK.get(cfg.block);
        if (block == null) return;
        BlockState newState = block.defaultBlockState();
        ctx.level().setBlock(ctx.pos(), newState, 3);
        if (cfg.nbt.isPresent()) {
            BlockEntity be = ctx.level().getBlockEntity(ctx.pos());
            if (be != null) {
                CompoundTag tag = cfg.nbt.get().tag().copy();
                tag.putInt("x", ctx.pos().getX());
                tag.putInt("y", ctx.pos().getY());
                tag.putInt("z", ctx.pos().getZ());
                be.load(tag);
                be.setChanged();
            }
        }
    }
}
