package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.block.RedstonePulses;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;

import java.util.List;

public final class RedstonePulseBlockAction implements ActionType<BlockCtx, RedstonePulseBlockAction.Cfg> {

    private static final Codec<RedstonePulses.Mode> MODE_CODEC = StringRepresentable.fromEnum(RedstonePulses.Mode::values);

    public record Cfg(RedstonePulses.Mode mode, int strength, int duration, boolean strong, List<Direction> sides) {
        int sideMask() {
            if (sides.isEmpty()) return RedstonePulses.ALL_SIDES;
            int mask = 0;
            for (Direction direction : sides) mask |= 1 << direction.get3DDataValue();
            return mask;
        }
    }

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        MODE_CODEC.optionalFieldOf("mode", RedstonePulses.Mode.RECEIVE).forGetter(Cfg::mode),
        Codec.intRange(1, 15).optionalFieldOf("strength", 15).forGetter(Cfg::strength),
        Codec.intRange(1, 72000).optionalFieldOf("duration", 4).forGetter(Cfg::duration),
        Codec.BOOL.optionalFieldOf("strong", false).forGetter(Cfg::strong),
        SingleOrList.of(Direction.CODEC).optionalFieldOf("sides", List.of()).forGetter(Cfg::sides)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        RedstonePulses.pulse(level, ctx.pos(), cfg.mode(), cfg.strength(), cfg.duration(), cfg.sideMask(), cfg.strong());
    }
}
