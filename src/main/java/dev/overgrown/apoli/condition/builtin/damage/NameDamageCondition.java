package dev.overgrown.apoli.condition.builtin.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;

public final class NameDamageCondition implements ConditionType<DamageCtx, NameDamageCondition.Cfg> {
    public record Cfg(String name) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Cfg::name)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, DamageCtx ctx) {
        return cfg.name.equals(ctx.source().getMsgId());
    }
}
