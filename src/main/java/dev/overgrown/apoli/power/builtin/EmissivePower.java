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

import java.util.List;

public final class EmissivePower extends PowerType<EmissivePower.Config> {

    public record Config(int luminance, boolean waterSensitive, boolean selfLit) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 15).optionalFieldOf("luminance", 15).forGetter(Config::luminance),
            Codec.BOOL.optionalFieldOf("water_sensitive", false).forGetter(Config::waterSensitive),
            Codec.BOOL.optionalFieldOf("self_lit", true).forGetter(Config::selfLit)
        ).apply(i, Config::new));
    }

    public static int luminanceOf(@Nullable Entity entity) {
        return scan(entity, false);
    }

    public static int selfLitLuminanceOf(@Nullable Entity entity) {
        return scan(entity, true);
    }

    public static boolean isWaterSensitive(@Nullable Entity entity) {
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.EMISSIVE);
        if (powers.isEmpty()) return false;
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (!cfg.waterSensitive) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            return true;
        }
        return false;
    }

    private static int scan(@Nullable Entity entity, boolean selfLitOnly) {
        if (entity == null) return 0;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return 0;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.EMISSIVE);
        if (powers.isEmpty()) return 0;
        int best = 0;
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (cfg.luminance <= best) continue;
            if (selfLitOnly && !cfg.selfLit) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            best = cfg.luminance;
            if (best >= 15) break;
        }
        return best;
    }
}
