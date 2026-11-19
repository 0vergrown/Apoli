package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.possession.Possession;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class PossessedCondition implements ConditionType<EntityCtx, PossessedCondition.Cfg> {
    public record Cfg(Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::bientityCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity host = ctx.raw();
        if (host == null) return false;
        Entity possessor = Possession.possessorOf(host);
        if (possessor == null) return false;
        return cfg.bientityCondition.isEmpty()
            || cfg.bientityCondition.get().test(new BiEntityCtx(possessor, host, ctx.level()));
    }
}
