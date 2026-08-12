package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

public final class PowerTypeCondition implements ConditionType<EntityCtx, PowerTypeCondition.Cfg> {
    public record Cfg(ResourceLocation powerType) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("power_type").forGetter(Cfg::powerType)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null) return false;
        ResourceLocation target = PowerTypeRegistry.resolveId(cfg.powerType);
        for (ResourceLocation id : container.allPowers()) {
            Power loaded = ApoliPowers.get(id);
            if (loaded == null) continue;
            if (container.isSuppressed(id)) continue;
            if (PowerTypeRegistry.resolveId(loaded.typeId()).equals(target)) return true;
        }
        return false;
    }
}
