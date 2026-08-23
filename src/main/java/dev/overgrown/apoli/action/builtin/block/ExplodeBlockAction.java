package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.DestructionType;
import dev.overgrown.apoli.data.ExplosionHelper;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class ExplodeBlockAction implements ActionType<BlockCtx, ExplodeBlockAction.Cfg> {
    public record Cfg(
        float power,
        DestructionType destructionType,
        Optional<BlockCondition> indestructible,
        Optional<BlockCondition> destructible,
        boolean createFire
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("power").forGetter(Cfg::power),
            DestructionType.CODEC.optionalFieldOf("destruction_type", DestructionType.BREAK).forGetter(Cfg::destructionType),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("indestructible", BlockCondition.CODEC).forGetter(Cfg::indestructible),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("destructible", BlockCondition.CODEC).forGetter(Cfg::destructible),
            Codec.BOOL.optionalFieldOf("create_fire", false).forGetter(Cfg::createFire)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        ExplosionHelper.detonate(
            ctx.level(), null,
            Vec3.atCenterOf(ctx.pos()),
            cfg.power, cfg.createFire, cfg.destructionType,
            cfg.indestructible, cfg.destructible
        );
    }
}
