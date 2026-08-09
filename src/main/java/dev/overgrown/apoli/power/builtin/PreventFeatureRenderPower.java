package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PreventFeatureRenderPower extends PowerType<PreventFeatureRenderPower.Config> {

    private static final String[] ALL = new String[0];

    public record Config(String[] features) {
        public boolean matches(String[] layerKeys) {
            if (features.length == 0) return true;
            for (int i = 0; i < layerKeys.length; i++) {
                String key = layerKeys[i];
                for (int j = 0; j < features.length; j++) {
                    if (features[j].equals(key)) return true;
                }
            }
            return false;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("feature").forGetter(c -> Optional.empty()),
            Codec.STRING.listOf().optionalFieldOf("features").forGetter(c -> Optional.of(List.of(c.features())))
        ).apply(i, (feature, features) -> {
            Set<String> merged = new LinkedHashSet<>();
            features.ifPresent(merged::addAll);
            feature.ifPresent(merged::add);
            return new Config(merged.isEmpty() ? ALL : merged.toArray(String[]::new));
        }));
    }

    public static boolean prevents(@Nullable Entity entity, String[] layerKeys) {
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.PREVENT_FEATURE_RENDER);
        if (powers.isEmpty()) return false;

        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(power.config() instanceof Config cfg)) continue;
            if (!cfg.matches(layerKeys)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            return true;
        }
        return false;
    }
}
