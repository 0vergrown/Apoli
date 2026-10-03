package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

public final class ZoomPower extends PowerType<ZoomPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("zoom");

    public record Config(Expression zoom, boolean scaleSensitivity, boolean cinematic, boolean hideHand, float speed) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.FLOAT_OR_EXPR.optionalFieldOf("zoom", Expression.constant(4)).forGetter(Config::zoom),
            Codec.BOOL.optionalFieldOf("scale_sensitivity", true).forGetter(Config::scaleSensitivity),
            Codec.BOOL.optionalFieldOf("cinematic", false).forGetter(Config::cinematic),
            Codec.BOOL.optionalFieldOf("hide_hand", false).forGetter(Config::hideHand),
            Codec.floatRange(0.01F, 1.0F).optionalFieldOf("speed", 0.5F).forGetter(Config::speed)
        ).apply(i, Config::new));
    }

    @Override
    public boolean resolvesForClient() {
        return true;
    }
}
