package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.data.BlockGlowStyle;
import dev.overgrown.apoli.power.PowerType;

public final class BlockGlowPower extends PowerType<BlockGlowPower.Config> {

    public static final double MAX_DISTANCE = 64.0;

    public record Config(BlockGlowStyle style, BlockCondition blockCondition, double distance) {
        public double range() {
            return Math.max(1.0, Math.min(MAX_DISTANCE, distance));
        }
    }

    private static final MapCodec<Config> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BlockGlowStyle.MAP_CODEC.forGetter(Config::style),
        BlockCondition.CODEC.fieldOf("block_condition").forGetter(Config::blockCondition),
        Codec.DOUBLE.optionalFieldOf("distance", 16.0).forGetter(Config::distance)
    ).apply(i, Config::new));

    @Override
    public MapCodec<Config> configCodec() {
        return CODEC;
    }

    @Override
    public boolean resolvesForClient() {
        return true;
    }
}
