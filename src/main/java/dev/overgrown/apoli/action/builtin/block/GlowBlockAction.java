package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.block.BlockGlows;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.BlockGlowStyle;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public final class GlowBlockAction implements ActionType<BlockCtx, GlowBlockAction.Cfg> {

    public record Cfg(BlockGlowStyle style, Optional<EntityCondition> entityCondition, int duration) {}

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BlockGlowStyle.MAP_CODEC.forGetter(Cfg::style),
        LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Cfg::entityCondition),
        Codec.INT.optionalFieldOf("duration", 20).forGetter(Cfg::duration)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        BlockGlows.glow(level, ctx.pos(), cfg);
    }
}
