package dev.overgrown.apoli.compat.voicechat;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.compat.voicechat.VoiceState;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;

public final class VoiceDisabledCondition implements ConditionType<EntityCtx, VoiceDisabledCondition.Cfg> {
    public record Cfg() {}

    private static final MapCodec<Cfg> CODEC = MapCodec.unit(new Cfg());

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        java.util.UUID uuid = ctx.entity().getUUID();
        return VoiceState.isDisabled(uuid) || VoiceState.isDisconnected(uuid);
    }
}
