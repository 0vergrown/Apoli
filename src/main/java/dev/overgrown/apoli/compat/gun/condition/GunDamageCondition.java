package dev.overgrown.apoli.compat.gun.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.compat.gun.GunFilter;
import dev.overgrown.apoli.compat.gun.GunHit;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;

import java.util.Optional;

public final class GunDamageCondition implements ConditionType<DamageCtx, GunDamageCondition.Cfg> {
    public record Cfg(GunFilter filter, Optional<Boolean> headshot) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            GunFilter.MAP_CODEC.forGetter(Cfg::filter),
            LoggedOptionalField.strict("headshot", Codec.BOOL).forGetter(Cfg::headshot)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, DamageCtx ctx) {
        GunHit.Info hit = GunHit.matching(ctx.source());
        if (hit == null || !cfg.filter().test(hit.gun())) return false;
        return cfg.headshot().isEmpty() || cfg.headshot().get() == hit.headshot();
    }
}
