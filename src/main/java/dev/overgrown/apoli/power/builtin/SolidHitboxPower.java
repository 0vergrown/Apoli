package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class SolidHitboxPower extends PowerType<SolidHitboxPower.Config> {
    public record Config(boolean pushable, Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("pushable", false).forGetter(Config::pushable),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }

    public static boolean isSolid(Entity holder) {
        return matching(holder, null) != null;
    }

    public static boolean isSolidFor(Entity holder, @Nullable Entity other) {
        return matching(holder, other) != null;
    }

    public static boolean isPushable(Entity holder) {
        Config cfg = matching(holder, null);
        return cfg == null || cfg.pushable();
    }

    private static @Nullable Config matching(Entity holder, @Nullable Entity other) {
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.isEmpty()) return null;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.SOLID_HITBOX);
        if (powers.isEmpty()) return null;
        EntityCtx selfCtx = null;
        BiEntityCtx pairCtx = null;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (selfCtx == null) selfCtx = EntityCtx.of(holder, holder.level());
                if (!power.condition().get().test(selfCtx)) continue;
            }
            if (cfg.bientityCondition().isEmpty() || other == null) return cfg;
            if (pairCtx == null) pairCtx = new BiEntityCtx(holder, other, holder.level());
            if (cfg.bientityCondition().get().test(pairCtx)) return cfg;
        }
        return null;
    }
}
