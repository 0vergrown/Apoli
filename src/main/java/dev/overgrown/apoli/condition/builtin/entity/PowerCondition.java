package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class PowerCondition implements ConditionType<EntityCtx, PowerCondition.Cfg> {
    public record Cfg(ResourceLocation power, Optional<ResourceLocation> source) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("power").forGetter(Cfg::power),
            IdCodecs.ID.optionalFieldOf("source").forGetter(Cfg::source)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null || !container.hasPower(cfg.power)) return false;
        if (cfg.source.isEmpty()) return true;
        return container.sourcesOf(cfg.power).contains(cfg.source.get());
    }
}
