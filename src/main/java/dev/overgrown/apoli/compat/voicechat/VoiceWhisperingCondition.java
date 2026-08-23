package dev.overgrown.apoli.compat.voicechat;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;

public final class VoiceWhisperingCondition implements ConditionType<EntityCtx, VoiceWhisperingCondition.Cfg> {
    public record Cfg() {}

    private static final MapCodec<Cfg> CODEC = MapCodec.unit(new Cfg());

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        return VoiceState.isWhispering(ctx.entity().getUUID());
    }
}
