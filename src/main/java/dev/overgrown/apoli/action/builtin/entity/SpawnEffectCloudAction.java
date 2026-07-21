package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.EffectSpec;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public final class SpawnEffectCloudAction implements ActionType<EntityCtx, SpawnEffectCloudAction.Cfg> {
    public record Cfg(
        float radius,
        float radiusOnUse,
        int waitTime,
        Optional<EffectSpec> effect,
        Optional<List<EffectSpec>> effects
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("radius", 3.0f).forGetter(Cfg::radius),
            Codec.FLOAT.optionalFieldOf("radius_on_use", -0.5f).forGetter(Cfg::radiusOnUse),
            Codec.INT.optionalFieldOf("wait_time", 10).forGetter(Cfg::waitTime),
            EffectSpec.CODEC.optionalFieldOf("effect").forGetter(Cfg::effect),
            Codec.list(EffectSpec.CODEC).optionalFieldOf("effects").forGetter(Cfg::effects)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity e = ctx.entity();
        AreaEffectCloud cloud = new AreaEffectCloud(e.level(), e.getX(), e.getY(), e.getZ());
        cloud.setOwner(e);
        cloud.setRadius(cfg.radius);
        cloud.setRadiusOnUse(cfg.radiusOnUse);
        cloud.setWaitTime(cfg.waitTime);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());
        cfg.effect.ifPresent(spec -> cloud.addEffect(spec.resolve(e)));
        cfg.effects.ifPresent(list -> list.forEach(spec -> cloud.addEffect(spec.resolve(e))));
        e.level().addFreshEntity(cloud);
    }
}
