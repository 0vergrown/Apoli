package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BonemealBlockAction implements ActionType<BlockCtx, BonemealBlockAction.Cfg> {
    public record Cfg(boolean effects) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("effects", true).forGetter(Cfg::effects)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return;
        BlockState state = ctx.state();
        if (!(state.getBlock() instanceof BonemealableBlock bonemealable)) return;
        if (!bonemealable.isValidBonemealTarget(serverLevel, ctx.pos(), state, false)) return;
        if (!bonemealable.isBonemealSuccess(serverLevel, serverLevel.random, ctx.pos(), state)) {
            return;
        }
        bonemealable.performBonemeal(serverLevel, serverLevel.random, ctx.pos(), state);
        if (cfg.effects) {
            serverLevel.levelEvent(1505, ctx.pos(), 15);
        }
    }
}
