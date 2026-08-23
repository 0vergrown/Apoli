package dev.overgrown.apoli.condition.builtin.damage;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public final class FixedTagDamageCondition implements ConditionType<DamageCtx, EmptyCfg> {
    private final TagKey<DamageType> tag;

    public FixedTagDamageCondition(TagKey<DamageType> tag) {
        this.tag = tag;
    }

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, DamageCtx ctx) {
        return ctx.source().is(this.tag);
    }
}
