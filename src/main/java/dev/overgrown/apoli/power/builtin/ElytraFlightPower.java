package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
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

public final class ElytraFlightPower extends PowerType<ElytraFlightPower.Config> {
    public record Config(boolean renderElytra, Optional<ResourceLocation> textureLocation) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("render_elytra").forGetter(Config::renderElytra),
            IdCodecs.ID.optionalFieldOf("texture_location").forGetter(Config::textureLocation)
        ).apply(i, Config::new));
    }

    public static boolean shouldRenderElytra(@Nullable Entity entity) {
        return rendered(entity) != null;
    }

    public static @Nullable ResourceLocation textureOf(@Nullable Entity entity) {
        Config cfg = rendered(entity);
        return cfg == null ? null : cfg.textureLocation().orElse(null);
    }

    private static @Nullable Config rendered(@Nullable Entity entity) {
        if (entity == null) return null;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return null;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.ELYTRA_FLIGHT);
        if (powers.isEmpty()) return null;
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (!cfg.renderElytra()) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            return cfg;
        }
        return null;
    }
}
