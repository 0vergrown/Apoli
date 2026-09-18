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
import dev.overgrown.apoli.data.ExprVector;
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
        Expression count,
        Either<Expression, ExprVector> speed,
        boolean force,
        ExprVector spread,
        Expression offsetX,
        Expression offsetY,
        Expression offsetZ,
        Expression frequency,
        boolean visibleInFirstPerson,
        boolean visibleWhileInvisible,
        Expression velocityX,
        Expression velocityY,
        Expression velocityZ,
        Optional<Space> space,
        Optional<String> modelPart
    ) {
        Config withMotion(Motion motion) {
            return new Config(particle, bientityCondition, count, speed, force, spread, offsetX, offsetY, offsetZ,
                frequency, visibleInFirstPerson, visibleWhileInvisible, motion.velocityX(), motion.velocityY(),
                motion.velocityZ(), motion.space(), motion.modelPart());
        }
    }

    private record Motion(Expression velocityX, Expression velocityY, Expression velocityZ, Optional<Space> space,
                          Optional<String> modelPart) {}

    private static final ExprVector DEFAULT_SPREAD = ExprVector.of(0.5f, 0.5f, 0.5f);
    private static final Expression ZERO = Expression.constant(0.0);
    private static final Expression HALF = Expression.constant(0.5);
    private static final Expression ONE = Expression.constant(1.0);

    private static final MapCodec<Config> BODY = RecordCodecBuilder.mapCodec(i -> i.group(
            ParticleEffect.CODEC.fieldOf("particle").forGetter(Config::particle),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            Expression.INT_OR_EXPR.optionalFieldOf("count", ONE).forGetter(Config::count),
            ParticlePlacement.SPEED_CODEC.optionalFieldOf("speed", ParticlePlacement.NO_SPEED).forGetter(Config::speed),
            Codec.BOOL.optionalFieldOf("force", false).forGetter(Config::force),
            ExprVector.SCALAR_OR_VECTOR.optionalFieldOf("spread", DEFAULT_SPREAD).forGetter(Config::spread),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_x", ZERO).forGetter(Config::offsetX),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_y", HALF).forGetter(Config::offsetY),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_z", ZERO).forGetter(Config::offsetZ),
            Expression.INT_OR_EXPR.fieldOf("frequency").forGetter(Config::frequency),
            Codec.BOOL.optionalFieldOf("visible_in_first_person", false).forGetter(Config::visibleInFirstPerson),
            Codec.BOOL.optionalFieldOf("visible_while_invisible", false).forGetter(Config::visibleWhileInvisible)
        ).apply(i, (particle, bientityCondition, count, speed, force, spread, offsetX, offsetY, offsetZ,
                    frequency, firstPerson, whileInvisible) ->
            new Config(particle, bientityCondition, count, speed, force, spread, offsetX, offsetY, offsetZ,
                frequency, firstPerson, whileInvisible, ZERO, ZERO, ZERO, Optional.empty(), Optional.empty())));

    private static final MapCodec<Motion> MOTION = RecordCodecBuilder.mapCodec(i -> i.group(
        Expression.FLOAT_OR_EXPR.optionalFieldOf("velocity_x", ZERO).forGetter(Motion::velocityX),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("velocity_y", ZERO).forGetter(Motion::velocityY),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("velocity_z", ZERO).forGetter(Motion::velocityZ),
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
        Entity owner = holder.rawOwner();
        if (!(owner.level() instanceof ServerLevel level)) return;
        int frequency = cfg.frequency().evalInt(owner);
        if (frequency < 1) return;
        if (owner.tickCount % frequency != 0) return;

        Power loaded = ApoliPowers.get(powerId);
        if (loaded != null && loaded.condition().isPresent()
            && !loaded.condition().get().test(EntityCtx.of(owner, level))) {
            return;
        }

        ParticleOptions opts = cfg.particle().resolve(level, owner);
        if (opts == null) return;

        dev.overgrown.apoli.data.ModelPartAnchor.Frame frame = ParticlePlacement.frame(owner, cfg.modelPart());
        Vec3 origin = ParticlePlacement.origin(owner, frame, cfg.space(),
            (float) cfg.offsetX().eval(owner), (float) cfg.offsetY().eval(owner), (float) cfg.offsetZ().eval(owner));
        Vector speedVector = ParticlePlacement.speedVector(owner, cfg.speed());
        Vec3 velocity = ParticlePlacement.velocity(owner, frame, cfg.space(),
            (float) cfg.velocityX().eval(owner), (float) cfg.velocityY().eval(owner),
            (float) cfg.velocityZ().eval(owner), speedVector);
        int count = Math.max(0, cfg.count().evalInt(owner));
        Vector spread = cfg.spread().resolve(owner);
        float scalarSpeed = ParticlePlacement.scalarSpeed(owner, cfg.speed());
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
                packets = ParticlePlacement.packets(opts, cfg.force(), origin, velocity, count, spread,
                    scalarSpeed, level, owner, frame, cfg.space());
            }
            for (int p = 0; p < packets.size(); p++) {
                dev.overgrown.apoli.data.ParticleBroadcast.send(level, player, packets.get(p));
            }
        }
    }
}
