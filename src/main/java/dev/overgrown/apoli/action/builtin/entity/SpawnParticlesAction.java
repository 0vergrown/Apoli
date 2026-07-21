package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.ParticleEffect;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class SpawnParticlesAction implements ActionType<EntityCtx, SpawnParticlesAction.Cfg> {
    public record Cfg(
        ParticleEffect particle,
        Optional<BiEntityCondition> bientityCondition,
        int count,
        float speed,
        boolean force,
        Vector spread,
        float offsetX,
        float offsetY,
        float offsetZ
    ) {}

    private static final Vector DEFAULT_SPREAD = new Vector(0.5f, 0.5f, 0.5f);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ParticleEffect.CODEC.fieldOf("particle").forGetter(Cfg::particle),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Cfg::bientityCondition),
            Codec.INT.fieldOf("count").forGetter(Cfg::count),
            Codec.FLOAT.optionalFieldOf("speed", 0f).forGetter(Cfg::speed),
            Codec.BOOL.optionalFieldOf("force", false).forGetter(Cfg::force),
            Vector.CODEC.optionalFieldOf("spread", DEFAULT_SPREAD).forGetter(Cfg::spread),
            Codec.FLOAT.optionalFieldOf("offset_x", 0f).forGetter(Cfg::offsetX),
            Codec.FLOAT.optionalFieldOf("offset_y", 0.5f).forGetter(Cfg::offsetY),
            Codec.FLOAT.optionalFieldOf("offset_z", 0f).forGetter(Cfg::offsetZ)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        Entity e = ctx.raw();
        ParticleOptions opts = cfg.particle.resolve(level);
        if (opts == null) return;

        double x = e.getX() + cfg.offsetX;
        double y = e.getY() + cfg.offsetY;
        double z = e.getZ() + cfg.offsetZ;
        for (ServerPlayer player : level.players()) {
            if (cfg.bientityCondition.isPresent()
                && !cfg.bientityCondition.get().test(BiEntityCtx.of(e, player, level))) continue;
            level.sendParticles(player, opts, cfg.force,
                x, y, z, cfg.count, cfg.spread.x(), cfg.spread.y(), cfg.spread.z(), cfg.speed);
        }
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
