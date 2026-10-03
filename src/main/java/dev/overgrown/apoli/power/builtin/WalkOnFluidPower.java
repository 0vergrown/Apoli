package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeUsage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.List;

public final class WalkOnFluidPower extends PowerType<WalkOnFluidPower.Config> {
    public static final PowerTypeUsage.Handle HELD = PowerTypeUsage.handle(ApoliIds.WALK_ON_FLUID);

    public record Config(TagKey<Fluid> fluid) {
        public TagKey<Fluid> fluidTag() {
            return fluid;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.<Fluid>tagKey(Registries.FLUID).fieldOf("fluid").forGetter(Config::fluid)
        ).apply(i, Config::new));
    }

    public static boolean standsOn(LivingEntity entity, FluidState fluid) {
        if (!entity.level().isClientSide() && !HELD.isHeld()) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.WALK_ON_FLUID);
        if (powers.isEmpty()) return false;
        EntityCtx ctx = null;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (!fluid.is(cfg.fluid)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            return true;
        }
        return false;
    }
}
