package dev.overgrown.apoli.condition.builtin.damage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.data.IdOrTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageType;

public final class TypeDamageCondition implements ConditionType<DamageCtx, TypeDamageCondition.Cfg> {
    public record Cfg(IdOrTag<DamageType> damageType) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdOrTag.codec(Registries.DAMAGE_TYPE).fieldOf("damage_type").forGetter(Cfg::damageType)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, DamageCtx ctx) {
        return cfg.damageType.matches(ctx.source().typeHolder());
    }
}
