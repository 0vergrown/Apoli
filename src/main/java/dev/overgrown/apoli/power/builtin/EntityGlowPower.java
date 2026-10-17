package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
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

public final class EntityGlowPower extends PowerType<EntityGlowPower.Config> {
    public record Config(
        Optional<EntityCondition> entityCondition,
        Optional<BiEntityCondition> bientityCondition,
        boolean useTeams,
        float red,
        float green,
        float blue,
        boolean selfGlowTarget
    ) {}

    private static volatile int cachedGeneration = -1;
    private static volatile boolean cachedInUse;

    public static boolean inUse() {
        int generation = ApoliPowers.generation();
        if (generation != cachedGeneration) {
            cachedInUse = ApoliPowers.anyOfType(ApoliIds.ENTITY_GLOW);
            cachedGeneration = generation;
        }
        return cachedInUse;
    }

    public static @Nullable Config match(Entity holder, Entity subject, Entity glowing, boolean selfGlow) {
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.isEmpty()) return null;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.ENTITY_GLOW);
        if (powers.isEmpty()) return null;
        EntityCtx holderCtx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg) || cfg.selfGlowTarget() != selfGlow) continue;
            if (power.condition().isPresent()) {
                if (holderCtx == null) holderCtx = EntityCtx.of(holder, holder.level());
                if (!power.condition().get().test(holderCtx)) continue;
            }
            if (cfg.entityCondition().isPresent()
                && !cfg.entityCondition().get().test(new EntityCtx(subject, subject.level()))) continue;
            if (cfg.bientityCondition().isPresent()
                && !cfg.bientityCondition().get().test(new BiEntityCtx(holder, subject, glowing.level()))) continue;
            return cfg;
        }
        return null;
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Config::entityCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            Codec.BOOL.optionalFieldOf("use_teams", true).forGetter(Config::useTeams),
            Codec.FLOAT.optionalFieldOf("red", 1.0f).forGetter(Config::red),
            Codec.FLOAT.optionalFieldOf("green", 1.0f).forGetter(Config::green),
            Codec.FLOAT.optionalFieldOf("blue", 1.0f).forGetter(Config::blue),
            Codec.BOOL.optionalFieldOf("self_glow_target", false).forGetter(Config::selfGlowTarget)
        ).apply(i, Config::new));
    }
}
