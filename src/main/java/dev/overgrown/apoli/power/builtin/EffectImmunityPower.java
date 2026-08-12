package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EffectImmunityPower extends PowerType<EffectImmunityPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("effect_immunity");

    public record Config(
        Optional<ResourceLocation> effect,
        Optional<List<ResourceLocation>> effects,
        boolean inverted
    ) {
        public boolean blocks(MobEffect target) {
            ResourceLocation targetId = BuiltInRegistries.MOB_EFFECT.getKey(target);
            if (targetId == null) return false;
            boolean listed = (effect.isPresent() && effect.get().equals(targetId))
                || (effects.isPresent() && effects.get().contains(targetId));
            if (effect.isEmpty() && effects.isEmpty()) return inverted;
            return inverted != listed;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("effect").forGetter(Config::effect),
            Codec.list(IdCodecs.ID).optionalFieldOf("effects").forGetter(Config::effects),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(Config::inverted)
        ).apply(i, Config::new));
    }

    public static boolean isImmuneTo(LivingEntity entity, MobEffectInstance effect) {
        MobEffect target = effect.getEffect();
        boolean[] blocked = new boolean[]{false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (blocked[0]) return;
            if (cfg.blocks(target)) blocked[0] = true;
        });
        return blocked[0];
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;
        if (owner.getActiveEffects().isEmpty()) return;

        Power loaded = ApoliPowers.get(powerId);
        if (loaded != null && loaded.condition().isPresent()
            && !loaded.condition().get().test(new EntityCtx(owner, level))) {
            return;
        }

        List<MobEffect> doomed = null;
        for (MobEffectInstance instance : owner.getActiveEffects()) {
            if (!cfg.blocks(instance.getEffect())) continue;
            if (doomed == null) doomed = new ArrayList<>(2);
            doomed.add(instance.getEffect());
        }
        if (doomed == null) return;
        for (int i = 0; i < doomed.size(); i++) {
            owner.removeEffect(doomed.get(i));
        }
    }
}
