package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class NightVisionPower extends PowerType<NightVisionPower.Config> {
    public record Config(float strength) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("strength", 1.0f).forGetter(Config::strength)
        ).apply(i, Config::new));
    }

    public static float strengthFor(@Nullable LivingEntity entity) {
        if (entity == null) return 0f;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return 0f;
        if (container.powersOfType(ApoliIds.NIGHT_VISION).isEmpty()) return 0f;
        float[] best = {0f};
        PowerLookup.forEach(entity, ApoliIds.NIGHT_VISION, Config.class, cfg -> {
            if (cfg.strength > best[0]) best[0] = cfg.strength;
        });
        return best[0];
    }
}
