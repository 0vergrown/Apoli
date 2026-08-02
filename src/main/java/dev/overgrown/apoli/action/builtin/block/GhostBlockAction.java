package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.block.GhostBlocks;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Nbt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;

public final class GhostBlockAction implements ActionType<BlockCtx, GhostBlockAction.Cfg> {
    public record Cfg(
        ResourceLocation block,
        Optional<Nbt> nbt,
        int duration,
        Optional<BlockAction> blockAction,
        Optional<BlockAction> endAction
    ) {}

    private static final MapCodec<Cfg> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        ResourceLocation.CODEC.fieldOf("block").forGetter(Cfg::block),
        Nbt.CODEC.optionalFieldOf("nbt").forGetter(Cfg::nbt),
        Codec.INT.optionalFieldOf("duration", 20).forGetter(Cfg::duration),
        BlockAction.CODEC.optionalFieldOf("block_action").forGetter(Cfg::blockAction),
        BlockAction.CODEC.optionalFieldOf("end_action").forGetter(Cfg::endAction)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(INNER, Map.of("tick", "duration"));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        Block block = BuiltInRegistries.BLOCK.get(cfg.block);
        if (block == null) return;
        BlockState state = block.defaultBlockState();
        GhostBlocks.place(level, ctx.pos(), state,
            cfg.nbt.map(n -> n.tag().copy()).orElse(null),
            cfg.duration, cfg.blockAction, cfg.endAction);
    }
}
