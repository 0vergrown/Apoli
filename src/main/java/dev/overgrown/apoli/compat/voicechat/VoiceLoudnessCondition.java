package dev.overgrown.apoli.compat.voicechat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.compat.voicechat.VoiceState;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;

public final class VoiceLoudnessCondition implements ConditionType<EntityCtx, VoiceLoudnessCondition.Cfg> {
    public record Cfg(Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to", 1).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        return cfg.comparison().compare(VoiceState.loudness(ctx.entity().getUUID()), cfg.compareTo());
    }
}
