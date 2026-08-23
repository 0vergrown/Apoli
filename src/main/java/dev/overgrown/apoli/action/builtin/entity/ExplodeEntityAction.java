package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.DestructionType;
import dev.overgrown.apoli.data.ExplosionHelper;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class ExplodeEntityAction implements ActionType<EntityCtx, ExplodeEntityAction.Cfg> {
    public record Cfg(
        float power,
        DestructionType destructionType,
        boolean damageSelf,
        Optional<BlockCondition> indestructible,
        Optional<BlockCondition> destructible,
        boolean createFire
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("power").forGetter(Cfg::power),
            DestructionType.CODEC.optionalFieldOf("destruction_type", DestructionType.BREAK).forGetter(Cfg::destructionType),
            Codec.BOOL.optionalFieldOf("damage_self", true).forGetter(Cfg::damageSelf),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("indestructible", BlockCondition.CODEC).forGetter(Cfg::indestructible),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("destructible", BlockCondition.CODEC).forGetter(Cfg::destructible),
            Codec.BOOL.optionalFieldOf("create_fire", false).forGetter(Cfg::createFire)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity holder = ctx.raw();
        boolean wasInvulnerable = holder.isInvulnerable();
        if (!cfg.damageSelf) holder.setInvulnerable(true);
        try {
            ExplosionHelper.detonate(
                ctx.level(), holder, holder.position(),
                cfg.power, cfg.createFire, cfg.destructionType,
                cfg.indestructible, cfg.destructible
            );
        } finally {
            if (!cfg.damageSelf) holder.setInvulnerable(wasInvulnerable);
        }
    }
}
