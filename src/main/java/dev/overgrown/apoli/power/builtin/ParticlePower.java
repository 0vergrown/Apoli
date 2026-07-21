package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.ParticleEffect;
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

import java.util.Optional;

public final class ParticlePower extends PowerType<ParticlePower.Config> {
    public record Config(
        ParticleEffect particle,
        Optional<BiEntityCondition> bientityCondition,
        int count,
        float speed,
        boolean force,
        Vector spread,
        float offsetX,
        float offsetY,
        float offsetZ,
        int frequency,
        boolean visibleInFirstPerson,
        boolean visibleWhileInvisible
    ) {}

    private static final Vector DEFAULT_SPREAD = new Vector(0.5f, 0.5f, 0.5f);

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ParticleEffect.CODEC.fieldOf("particle").forGetter(Config::particle),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            Codec.INT.optionalFieldOf("count", 1).forGetter(Config::count),
            Codec.FLOAT.optionalFieldOf("speed", 0f).forGetter(Config::speed),
            Codec.BOOL.optionalFieldOf("force", false).forGetter(Config::force),
            Vector.CODEC.optionalFieldOf("spread", DEFAULT_SPREAD).forGetter(Config::spread),
            Codec.FLOAT.optionalFieldOf("offset_x", 0f).forGetter(Config::offsetX),
            Codec.FLOAT.optionalFieldOf("offset_y", 0.5f).forGetter(Config::offsetY),
            Codec.FLOAT.optionalFieldOf("offset_z", 0f).forGetter(Config::offsetZ),
            Codec.INT.fieldOf("frequency").forGetter(Config::frequency),
            Codec.BOOL.optionalFieldOf("visible_in_first_person", false).forGetter(Config::visibleInFirstPerson),
            Codec.BOOL.optionalFieldOf("visible_while_invisible", false).forGetter(Config::visibleWhileInvisible)
        ).apply(i, Config::new));
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

        double x = owner.getX() + cfg.offsetX();
        double y = owner.getY() + cfg.offsetY();
        double z = owner.getZ() + cfg.offsetZ();
        for (ServerPlayer player : level.players()) {
            if (!cfg.visibleWhileInvisible() && owner.isInvisibleTo(player)) continue;
            if (cfg.bientityCondition().isPresent() && owner instanceof LivingEntity le
                && !cfg.bientityCondition().get().test(new BiEntityCtx(le, player, level))) continue;
            level.sendParticles(player, opts, cfg.force(),
                x, y, z, cfg.count(), cfg.spread().x(), cfg.spread().y(), cfg.spread().z(), cfg.speed());
        }
    }
}
