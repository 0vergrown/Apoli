package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.data.ParticleEffect;
import dev.overgrown.apoli.data.ParticlePlacement;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class SpawnParticlesAction implements ActionType<EntityCtx, SpawnParticlesAction.Cfg> {
    public record Cfg(
        ParticleEffect particle,
        Optional<BiEntityCondition> bientityCondition,
        int count,
        Either<Expression, Vector> speed,
        boolean force,
        Vector spread,
        float offsetX,
        float offsetY,
        float offsetZ,
        float velocityX,
        float velocityY,
        float velocityZ,
        Optional<Space> space,
        Optional<String> modelPart
    ) {}

    private static final Vector DEFAULT_SPREAD = new Vector(0.5f, 0.5f, 0.5f);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ParticleEffect.CODEC.fieldOf("particle").forGetter(Cfg::particle),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::bientityCondition),
            Codec.INT.fieldOf("count").forGetter(Cfg::count),
            ParticlePlacement.SPEED_CODEC.optionalFieldOf("speed", ParticlePlacement.NO_SPEED).forGetter(Cfg::speed),
            Codec.BOOL.optionalFieldOf("force", false).forGetter(Cfg::force),
            Vector.CODEC.optionalFieldOf("spread", DEFAULT_SPREAD).forGetter(Cfg::spread),
            Codec.FLOAT.optionalFieldOf("offset_x", 0f).forGetter(Cfg::offsetX),
            Codec.FLOAT.optionalFieldOf("offset_y", 0.5f).forGetter(Cfg::offsetY),
            Codec.FLOAT.optionalFieldOf("offset_z", 0f).forGetter(Cfg::offsetZ),
            Codec.FLOAT.optionalFieldOf("velocity_x", 0f).forGetter(Cfg::velocityX),
            Codec.FLOAT.optionalFieldOf("velocity_y", 0f).forGetter(Cfg::velocityY),
            Codec.FLOAT.optionalFieldOf("velocity_z", 0f).forGetter(Cfg::velocityZ),
            Space.CODEC.optionalFieldOf("space").forGetter(Cfg::space),
            ModelParts.NAME_CODEC.optionalFieldOf("model_part").forGetter(Cfg::modelPart)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        Entity e = ctx.raw();
        if (e == null) return;
        ParticleOptions opts = cfg.particle.resolve(level);
        if (opts == null) return;

        dev.overgrown.apoli.data.ModelPartAnchor.Frame frame = ParticlePlacement.frame(e, cfg.modelPart);
        Vec3 origin = ParticlePlacement.origin(e, frame, cfg.space, cfg.offsetX, cfg.offsetY, cfg.offsetZ);
        Vec3 velocity = ParticlePlacement.velocity(e, frame, cfg.space, cfg.velocityX, cfg.velocityY, cfg.velocityZ,
            cfg.speed);
        List<Packet<?>> packets = null;
        List<ServerPlayer> players = level.players();
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            if (!dev.overgrown.apoli.data.ParticleBroadcast.inRange(player, cfg.force, origin.x, origin.y, origin.z)) continue;
            if (cfg.bientityCondition.isPresent()
                && !cfg.bientityCondition.get().test(BiEntityCtx.of(e, player, level))) continue;
            if (packets == null) {
                packets = ParticlePlacement.packets(opts, cfg.force, origin, velocity, cfg.count, cfg.spread,
                    ParticlePlacement.scalarSpeed(e, cfg.speed), level);
            }
            for (int p = 0; p < packets.size(); p++) {
                dev.overgrown.apoli.data.ParticleBroadcast.send(level, player, packets.get(p));
            }
        }
    }
}
