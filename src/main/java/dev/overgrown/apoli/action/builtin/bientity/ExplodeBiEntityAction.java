package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.DestructionType;
import dev.overgrown.apoli.data.ExplosionHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class ExplodeBiEntityAction implements ActionType<BiEntityCtx, ExplodeBiEntityAction.Cfg> {
    public record Cfg(
        float power,
        DestructionType destructionType,
        boolean createFire,
        boolean atTarget,
        boolean damageSelf,
        boolean damageTargets,
        Optional<BlockCondition> indestructible,
        Optional<BlockCondition> destructible,
        Optional<BiEntityCondition> bientityCondition
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("power").forGetter(Cfg::power),
            DestructionType.CODEC.optionalFieldOf("destruction_type", DestructionType.DESTROY).forGetter(Cfg::destructionType),
            Codec.BOOL.optionalFieldOf("create_fire", false).forGetter(Cfg::createFire),
            Codec.BOOL.optionalFieldOf("at_target", false).forGetter(Cfg::atTarget),
            Codec.BOOL.optionalFieldOf("damage_self", true).forGetter(Cfg::damageSelf),
            Codec.BOOL.optionalFieldOf("damage_targets", true).forGetter(Cfg::damageTargets),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("indestructible", BlockCondition.CODEC).forGetter(Cfg::indestructible),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("destructible", BlockCondition.CODEC).forGetter(Cfg::destructible),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::bientityCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity origin = cfg.atTarget ? ctx.target() : actor;
        if (origin == null) return;
        Vec3 pos = origin.position();
        boolean spareActor = !cfg.damageSelf && actor != null;
        boolean wasInvulnerable = actor != null && actor.isInvulnerable();
        if (spareActor) actor.setInvulnerable(true);
        try {
            ExplosionHelper.detonate(
                ctx.level(), actor, pos,
                cfg.power, cfg.createFire, cfg.destructionType,
                cfg.indestructible, cfg.destructible, cfg.damageTargets, cfg.bientityCondition
            );
        } finally {
            if (spareActor) actor.setInvulnerable(wasInvulnerable);
        }
    }
}
