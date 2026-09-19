package dev.overgrown.apoli.condition.builtin.damage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.attribution.PowerCause;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class SourceDamageCondition implements ConditionType<DamageCtx, SourceDamageCondition.Cfg> {

    public record Cfg(Optional<ResourceLocation> power, Optional<ResourceLocation> source) {
        public Cfg {
            PowerCause.armAttribution();
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("power").forGetter(Cfg::power),
            IdCodecs.ID.optionalFieldOf("source").forGetter(Cfg::source)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, DamageCtx ctx) {
        ResourceLocation causedBy = PowerCause.powerId();
        if (causedBy == null) return false;
        if (cfg.power.isPresent() && !cfg.power.get().equals(causedBy)) return false;
        if (cfg.source.isEmpty()) return true;
        Entity holder = PowerCause.holder();
        if (holder == null) return false;
        PowerContainer container = PowerContainer.of(holder);
        return container != null && container.sourcesOf(causedBy).contains(cfg.source.get());
    }
}
