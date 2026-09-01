package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.data.ParticleEffect;
import dev.overgrown.apoli.data.ParticlePlacement;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.data.Vector;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class ParticlePower extends PowerType<ParticlePower.Config> {
    public record Config(
        ParticleEffect particle,
        Optional<BiEntityCondition> bientityCondition,
        int count,
        Either<Expression, Vector> speed,
        boolean force,
        Vector spread,
        float offsetX,
        float offsetY,
        float offsetZ,
        int frequency,
        boolean visibleInFirstPerson,
        boolean visibleWhileInvisible,
        float velocityX,
        float velocityY,
        float velocityZ,
        Optional<Space> space,
        Optional<String> modelPart
    ) {
        Config withMotion(Motion motion) {
            return new Config(particle, bientityCondition, count, speed, force, spread, offsetX, offsetY, offsetZ,
                frequency, visibleInFirstPerson, visibleWhileInvisible, motion.velocityX(), motion.velocityY(),
                motion.velocityZ(), motion.space(), motion.modelPart());
        }
    }

    private record Motion(float velocityX, float velocityY, float velocityZ, Optional<Space> space,
                          Optional<String> modelPart) {}

    private static final Vector DEFAULT_SPREAD = new Vector(0.5f, 0.5f, 0.5f);

    private static final MapCodec<Config> BODY = RecordCodecBuilder.mapCodec(i -> i.group(
            ParticleEffect.CODEC.fieldOf("particle").forGetter(Config::particle),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            Codec.INT.optionalFieldOf("count", 1).forGetter(Config::count),
            ParticlePlacement.SPEED_CODEC.optionalFieldOf("speed", ParticlePlacement.NO_SPEED).forGetter(Config::speed),
            Codec.BOOL.optionalFieldOf("force", false).forGetter(Config::force),
            Vector.CODEC.optionalFieldOf("spread", DEFAULT_SPREAD).forGetter(Config::spread),
            Codec.FLOAT.optionalFieldOf("offset_x", 0f).forGetter(Config::offsetX),
            Codec.FLOAT.optionalFieldOf("offset_y", 0.5f).forGetter(Config::offsetY),
            Codec.FLOAT.optionalFieldOf("offset_z", 0f).forGetter(Config::offsetZ),
            Codec.INT.fieldOf("frequency").forGetter(Config::frequency),
            Codec.BOOL.optionalFieldOf("visible_in_first_person", false).forGetter(Config::visibleInFirstPerson),
            Codec.BOOL.optionalFieldOf("visible_while_invisible", false).forGetter(Config::visibleWhileInvisible)
        ).apply(i, (particle, bientityCondition, count, speed, force, spread, offsetX, offsetY, offsetZ,
                    frequency, firstPerson, whileInvisible) ->
            new Config(particle, bientityCondition, count, speed, force, spread, offsetX, offsetY, offsetZ,
                frequency, firstPerson, whileInvisible, 0f, 0f, 0f, Optional.empty(), Optional.empty())));

    private static final MapCodec<Motion> MOTION = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.FLOAT.optionalFieldOf("velocity_x", 0f).forGetter(Motion::velocityX),
        Codec.FLOAT.optionalFieldOf("velocity_y", 0f).forGetter(Motion::velocityY),
        Codec.FLOAT.optionalFieldOf("velocity_z", 0f).forGetter(Motion::velocityZ),
        Space.CODEC.optionalFieldOf("space").forGetter(Motion::space),
        ModelParts.NAME_CODEC.optionalFieldOf("model_part").forGetter(Motion::modelPart)
    ).apply(i, Motion::new));

    private static final MapCodec<Config> CODEC = Codec.mapPair(BODY, MOTION).xmap(
        pair -> pair.getFirst().withMotion(pair.getSecond()),
        config -> Pair.of(config, new Motion(config.velocityX(), config.velocityY(), config.velocityZ(),
            config.space(), config.modelPart())));

    @Override
    public MapCodec<Config> configCodec() {
        return CODEC;
    }

    @Override
    public boolean ticksNonLivingEntities() {
        return true;
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (cfg.frequency() < 1) return;
        Entity owner = holder.rawOwner();
        if (!(owner.level() instanceof ServerLevel level)) return;
        if (owner.tickCount % cfg.frequency() != 0) return;

        Power loaded = ApoliPowers.get(powerId);
        if (loaded != null && loaded.condition().isPresent()
            && !loaded.condition().get().test(EntityCtx.of(owner, level))) {
            return;
        }

        ParticleOptions opts = cfg.particle().resolve(level);
        if (opts == null) return;

        dev.overgrown.apoli.data.ModelPartAnchor.Frame frame = ParticlePlacement.frame(owner, cfg.modelPart());
        Vec3 origin = ParticlePlacement.origin(owner, frame, cfg.space(), cfg.offsetX(), cfg.offsetY(), cfg.offsetZ());
        Vec3 velocity = ParticlePlacement.velocity(owner, frame, cfg.space(), cfg.velocityX(), cfg.velocityY(),
            cfg.velocityZ(), cfg.speed());
        double x = origin.x;
        double y = origin.y;
        double z = origin.z;
        List<net.minecraft.network.protocol.Packet<?>> packets = null;
        List<ServerPlayer> players = level.players();
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            if (!dev.overgrown.apoli.data.ParticleBroadcast.inRange(player, cfg.force(), x, y, z)) continue;
            if (player == owner && !cfg.visibleInFirstPerson()
                && dev.overgrown.apoli.entity.CameraPerspectives.isFirstPerson(player)) continue;
            if (!cfg.visibleWhileInvisible() && owner.isInvisibleTo(player)) continue;
            if (cfg.bientityCondition().isPresent() && owner instanceof LivingEntity le
                && !cfg.bientityCondition().get().test(new BiEntityCtx(le, player, level))) continue;
            if (packets == null) {
                packets = ParticlePlacement.packets(opts, cfg.force(), origin, velocity, cfg.count(), cfg.spread(),
                    ParticlePlacement.scalarSpeed(owner, cfg.speed()), level);
            }
            for (int p = 0; p < packets.size(); p++) {
                dev.overgrown.apoli.data.ParticleBroadcast.send(level, player, packets.get(p));
            }
        }
    }
}
