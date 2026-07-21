package dev.overgrown.apoli.power;

import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PowerLookup {
    private PowerLookup() {}

    public static boolean hasActive(@Nullable Entity entity, ResourceLocation canonicalId) {
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(canonicalId);
        if (powers.isEmpty()) return false;
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static <C> void forEach(@Nullable Entity entity, ResourceLocation canonicalId,
                                   Class<C> configClass, Consumer<C> consumer) {
        if (entity == null) return;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return;
        List<ResourceLocation> powers = container.powersOfType(canonicalId);
        if (powers.isEmpty()) return;
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            Object cfg = power.config();
            if (!configClass.isInstance(cfg)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            consumer.accept((C) cfg);
        }
    }

    public static <C> List<C> active(@Nullable Entity entity, ResourceLocation canonicalId,
                                     Class<C> configClass) {
        List<C>[] result = new List[]{List.of()};
        forEach(entity, canonicalId, configClass, c -> {
            if (result[0] == List.<C>of()) result[0] = new ArrayList<>();
            result[0].add(c);
        });
        return result[0];
    }
}
