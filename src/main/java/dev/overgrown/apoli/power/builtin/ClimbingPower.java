package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeUsage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public final class ClimbingPower extends PowerType<ClimbingPower.Config> {
    public static final PowerTypeUsage.Handle HELD = PowerTypeUsage.handle(ApoliIds.CLIMBING);

    public static final int NONE = 0;
    public static final int CLIMBS = 1;
    public static final int HOLDS = 2;

    public record Config(boolean allowHolding, Optional<EntityCondition> holdCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("allow_holding", true).forGetter(Config::allowHolding),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("hold_condition", EntityCondition.CODEC).forGetter(Config::holdCondition)
        ).apply(i, Config::new));
    }

    public static boolean mayHold(LivingEntity entity) {
        return entity.level().isClientSide() || HELD.isHeld();
    }

    public static int state(LivingEntity entity, boolean resolveHold) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return NONE;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.CLIMBING);
        if (powers.isEmpty()) return NONE;
        EntityCtx ctx = null;
        int state = NONE;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            state = CLIMBS;
            if (!resolveHold) return CLIMBS;
            if (!cfg.allowHolding()) continue;
            boolean held;
            if (cfg.holdCondition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                held = cfg.holdCondition().get().test(ctx);
            } else {
                held = entity.isShiftKeyDown();
            }
            if (held) return HOLDS;
        }
        return state;
    }
}
