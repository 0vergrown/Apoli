package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.scale.ScaleState;
import dev.overgrown.apoli.scale.ScaleType;
import dev.overgrown.apoli.scale.ScaleTypeCodec;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class ScalePower extends PowerType<ScalePower.Config> {

    public record Config(
        List<ScaleType> types,
        Optional<Expression> scale,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers
    ) {
        public List<AttributeModifier> allModifiers() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ScaleTypeCodec.LIST_OR_SINGLE.optionalFieldOf("scale_types", List.of(ScaleTypes.BASE)).forGetter(Config::types),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("scale").forGetter(Config::scale),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        Entity owner = holder.rawOwner();
        if (owner != null) Scales.invalidate(owner);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        Entity owner = holder.rawOwner();
        if (owner != null) Scales.invalidate(owner);
    }

    @Override
    public void onSuppressed(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (owner != null) Scales.invalidate(owner);
    }

    @Override
    public void onUnsuppressed(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (owner != null) Scales.invalidate(owner);
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (owner == null) return;
        Scales.refreshDimensions(owner, Scales.stateOrCreate(owner));
    }

    @Override
    public boolean ticksNonLivingEntities() {
        return true;
    }

    public static boolean hasAny(Entity entity) {
        PowerContainer container = PowerContainer.of(entity);
        return container != null && !container.isEmpty()
            && !container.powersOfType(ApoliIds.SCALE).isEmpty();
    }

    public static boolean applyFactors(Entity entity, float[] out) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.SCALE);
        if (powers.isEmpty()) return false;
        Level level = entity.level();
        EntityCtx ctx = null;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, level);
                if (!power.condition().get().test(ctx)) continue;
            }
            float factor = factorOf(cfg, entity, container);
            if (factor == 1.0F) continue;
            List<ScaleType> types = cfg.types();
            for (int t = 0, tn = types.size(); t < tn; t++) {
                out[types.get(t).index()] *= factor;
            }
        }
        return true;
    }

    private static float factorOf(Config cfg, Entity entity, @Nullable PowerContainer container) {
        double factor = 1.0;
        if (cfg.scale().isPresent()) {
            factor = cfg.scale().get().evalWith(entity, container, 1.0);
        }
        List<AttributeModifier> mods = cfg.allModifiers();
        if (!mods.isEmpty()) {
            factor = AttributeModifierHelper.apply(factor, mods, entity, container);
        }
        return ScaleState.clamp((float) factor);
    }
}
