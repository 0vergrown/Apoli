package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.StatusEffectInstanceCodec;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public final class StackingStatusEffectPower extends PowerType<StackingStatusEffectPower.Config> {
    public record Config(
        int minStacks,
        int maxStacks,
        int durationPerStack,
        int tickRate,
        Optional<MobEffectInstance> effect,
        Optional<List<MobEffectInstance>> effects
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("min_stacks").forGetter(Config::minStacks),
            Codec.INT.fieldOf("max_stacks").forGetter(Config::maxStacks),
            Codec.INT.fieldOf("duration_per_stack").forGetter(Config::durationPerStack),
            Codec.INT.optionalFieldOf("tick_rate", 10).forGetter(Config::tickRate),
            StatusEffectInstanceCodec.CODEC.optionalFieldOf("effect").forGetter(Config::effect),
            Codec.list(StatusEffectInstanceCodec.CODEC).optionalFieldOf("effects").forGetter(Config::effects)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (impl.getAuxInt(powerId).isPresent()) return;
        impl.setAuxInt(powerId, Math.max(cfg.minStacks, Math.min(cfg.maxStacks, 0)));
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (!holder.hasPower(powerId)) impl.removeAux(powerId);
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (cfg.tickRate <= 0) return;
        LivingEntity owner = holder.owner();
        if (!(owner.level() instanceof ServerLevel level)) return;
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (owner.tickCount % cfg.tickRate != 0) return;
        int current = impl.getAuxInt(powerId).orElse(0);
        boolean active = conditionActive(powerId, owner, level);
        int next = active ? current + 1 : current - 1;
        next = Math.max(cfg.minStacks, Math.min(cfg.maxStacks, next));
        if (next != current) impl.setAuxInt(powerId, next);
        if (next > 0) reapplyEffects(owner, cfg, next);
    }

    private static void reapplyEffects(LivingEntity owner, Config cfg, int stacks) {
        int duration = stacks * cfg.durationPerStack;
        cfg.effect.ifPresent(e -> owner.addEffect(rebuild(e, duration)));
        cfg.effects.ifPresent(list -> list.forEach(e -> owner.addEffect(rebuild(e, duration))));
    }

    private static MobEffectInstance rebuild(MobEffectInstance template, int duration) {
        return new MobEffectInstance(
            template.getEffect(),
            duration,
            template.getAmplifier(),
            template.isAmbient(),
            template.isVisible(),
            template.showIcon()
        );
    }

    private static boolean conditionActive(ResourceLocation powerId, LivingEntity owner, ServerLevel level) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return false;
        if (loaded.condition().isEmpty()) return true;
        return loaded.condition().get().test(new EntityCtx(owner, level));
    }
}
