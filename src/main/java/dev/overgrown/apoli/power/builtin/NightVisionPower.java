package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class NightVisionPower extends PowerType<NightVisionPower.Config> {
    public record Config(Expression strength) {}

    private static final ThreadLocal<List<Config>> SCRATCH = ThreadLocal.withInitial(() -> new ArrayList<>(2));

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.FLOAT_OR_EXPR.optionalFieldOf("strength", Expression.constant(1.0)).forGetter(Config::strength)
        ).apply(i, Config::new));
    }

    public static float strengthFor(@Nullable LivingEntity entity) {
        if (entity == null) return 0f;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return 0f;
        if (container.powersOfType(ApoliIds.NIGHT_VISION).isEmpty()) return 0f;
        List<Config> active = SCRATCH.get();
        active.clear();
        PowerLookup.collect(entity, ApoliIds.NIGHT_VISION, Config.class, active);
        float best = 0f;
        for (int i = 0; i < active.size(); i++) {
            float strength = (float) active.get(i).strength().eval(entity);
            if (strength > best) best = strength;
        }
        active.clear();
        return best;
    }

    public static float effectScale(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(MobEffects.NIGHT_VISION);
        if (effect == null) return 0f;
        if (!effect.endsWithin(200)) return 1f;
        return 0.7F + Mth.sin(effect.getDuration() * (float) Math.PI * 0.2F) * 0.3F;
    }
}
