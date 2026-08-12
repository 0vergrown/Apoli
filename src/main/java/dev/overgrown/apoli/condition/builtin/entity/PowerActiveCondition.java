package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.TogglePower;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

public final class PowerActiveCondition implements ConditionType<EntityCtx, PowerActiveCondition.Cfg> {
    public record Cfg(ResourceLocation power) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("power").forGetter(Cfg::power)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null || !container.hasPower(cfg.power)) return false;
        if (container.isSuppressed(cfg.power)) return false;
        Power loaded = ApoliPowers.get(cfg.power);
        if (loaded == null) return false;
        if (loaded.condition().isPresent() && !loaded.condition().get().test(ctx)) return false;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type instanceof TogglePower && !TogglePower.isActive(ctx.entity(), cfg.power)) {
            return false;
        }
        return true;
    }
}
