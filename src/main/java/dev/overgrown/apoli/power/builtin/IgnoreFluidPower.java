package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.FluidCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.FluidCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeUsage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;

import java.util.List;
import java.util.Optional;

public final class IgnoreFluidPower extends PowerType<IgnoreFluidPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("ignore_fluid");
    public static final PowerTypeUsage.Handle HELD = PowerTypeUsage.handle(ApoliIds.IGNORE_FLUID);

    public record Config(Optional<FluidCondition> fluidCondition) {
        boolean matches(FluidState fluid, FluidCtx ctx) {
            return fluidCondition.isPresent() ? fluidCondition.get().test(ctx) : fluid.is(FluidTags.WATER);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("fluid_condition", FluidCondition.CODEC)
                .forGetter(Config::fluidCondition)
        ).apply(i, Config::new));
    }

    public static boolean mayIgnore(Entity entity) {
        return entity.level().isClientSide() ? entity.isControlledByLocalInstance() : HELD.isHeld();
    }

    public static boolean holds(Entity entity) {
        PowerContainer container = PowerContainer.of(entity);
        return container != null && !container.isEmpty() && !container.powersOfType(ApoliIds.IGNORE_FLUID).isEmpty();
    }

    public static boolean ignores(Entity entity, FluidState fluid, BlockPos pos) {
        if (fluid.isEmpty()) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.IGNORE_FLUID);
        if (powers.isEmpty()) return false;
        EntityCtx ctx = null;
        FluidCtx fluidCtx = null;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            if (fluidCtx == null) fluidCtx = new FluidCtx(fluid, pos.immutable(), entity.level());
            if (cfg.matches(fluid, fluidCtx)) return true;
        }
        return false;
    }
}
