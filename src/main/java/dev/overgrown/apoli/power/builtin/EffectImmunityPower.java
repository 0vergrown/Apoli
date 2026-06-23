package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

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
            ResourceLocation.CODEC.optionalFieldOf("effect").forGetter(Config::effect),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("effects").forGetter(Config::effects),
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
}
