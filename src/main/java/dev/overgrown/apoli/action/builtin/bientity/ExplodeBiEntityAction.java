package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
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
        Optional<BlockCondition> indestructible,
        Optional<BlockCondition> destructible
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("power").forGetter(Cfg::power),
            DestructionType.CODEC.optionalFieldOf("destruction_type", DestructionType.DESTROY).forGetter(Cfg::destructionType),
            Codec.BOOL.optionalFieldOf("create_fire", false).forGetter(Cfg::createFire),
            Codec.BOOL.optionalFieldOf("at_target", false).forGetter(Cfg::atTarget),
            BlockCondition.CODEC.optionalFieldOf("indestructible").forGetter(Cfg::indestructible),
            BlockCondition.CODEC.optionalFieldOf("destructible").forGetter(Cfg::destructible)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity origin = cfg.atTarget ? ctx.target() : ctx.actor();
        if (origin == null) return;
        Vec3 pos = origin.position();
        ExplosionHelper.detonate(
            ctx.level(), ctx.actor(), pos,
            cfg.power, cfg.createFire, cfg.destructionType,
            cfg.indestructible, cfg.destructible
        );
    }
}
