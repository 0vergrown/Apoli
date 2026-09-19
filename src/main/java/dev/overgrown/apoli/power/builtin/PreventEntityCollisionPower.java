package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;

public final class PreventEntityCollisionPower extends PowerType<PreventEntityCollisionPower.Config> {
    public record Config(Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }

    public static boolean prevents(Entity a, Entity b) {
        if (a == null || b == null || a == b) return false;
        return holderPrevents(a, b) || holderPrevents(b, a);
    }

    private static boolean holderPrevents(Entity holder, Entity other) {
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.PREVENT_ENTITY_COLLISION);
        if (powers.isEmpty()) return false;
        EntityCtx selfCtx = null;
        BiEntityCtx pairCtx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (selfCtx == null) selfCtx = EntityCtx.of(holder, holder.level());
                if (!power.condition().get().test(selfCtx)) continue;
            }
            if (cfg.bientityCondition.isEmpty()) return true;
            if (pairCtx == null) pairCtx = new BiEntityCtx(holder, other, holder.level());
            if (cfg.bientityCondition.get().test(pairCtx)) return true;
        }
        return false;
    }
}
