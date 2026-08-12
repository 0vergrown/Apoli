package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class ClearEffectAction implements ActionType<EntityCtx, ClearEffectAction.Cfg> {
    public record Cfg(Optional<ResourceLocation> effect) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("effect").forGetter(Cfg::effect)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity living = ctx.living();
        if (living == null) return;
        if (cfg.effect.isEmpty()) {
            living.removeAllEffects();
            return;
        }
        MobEffect target = BuiltInRegistries.MOB_EFFECT.get(cfg.effect.get());
        if (target != null) living.removeEffect(target);
    }
}
