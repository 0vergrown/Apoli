package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.ItemStackData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;

import java.util.Optional;

public final class BreakBlockAction implements ActionType<BlockCtx, BreakBlockAction.Cfg> {
    public record Cfg(boolean dropLoot, Optional<ItemStackData> stack, boolean breakParticles, boolean asPlayer) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("drop_loot", true).forGetter(Cfg::dropLoot),
            LoggedOptionalField.strict("stack", ItemStackData.CODEC).forGetter(Cfg::stack),
            Codec.BOOL.optionalFieldOf("break_particles", true).forGetter(Cfg::breakParticles),
            Codec.BOOL.optionalFieldOf("as_player", false).forGetter(Cfg::asPlayer)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        Level level = ctx.level();
        if (level.isClientSide) return;
        BlockPos pos = ctx.pos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        Entity actor = ctx.actor();
        ServerPlayer breaker = cfg.asPlayer && actor instanceof ServerPlayer player ? player : null;

        if (cfg.breakParticles) level.levelEvent(2001, pos, Block.getId(state));
        if (cfg.dropLoot) {
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            ItemStack tool = cfg.stack.map(ItemStackData::stack).map(ItemStack::copy).orElse(ItemStack.EMPTY);
            if (breaker != null) state.getBlock().playerDestroy(level, breaker, pos, state, blockEntity, tool);
            else Block.dropResources(state, level, pos, blockEntity, actor, tool);
        }
        FluidState fluid = level.getFluidState(pos);
        if (level.setBlock(pos, fluid.createLegacyBlock(), 3)) {
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(actor, state));
        }
    }
}
