package dev.overgrown.apoli.compat.icarus;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class WingsAccess {
    private WingsAccess() {}

    private static final ResourceLocation WINGS_ID = Apoli.id("wings");
    private static final long FRESH_TICKS = 2L;

    private record Entry(WingsPower.Config cfg, long tick) {}

    private static final Map<LivingEntity, Entry> SERVER_CACHE = new WeakHashMap<>();

    static void refresh(LivingEntity entity, WingsPower.Config cfg) {
        SERVER_CACHE.put(entity, new Entry(cfg, entity.level().getGameTime()));
    }

    static void invalidate(LivingEntity entity) {
        SERVER_CACHE.remove(entity);
    }

    public static void clear() {
        SERVER_CACHE.clear();
    }

    public static @Nullable WingsPower.Config get(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return resolveClient(entity);
        }
        Entry entry = SERVER_CACHE.get(entity);
        if (entry == null) return null;
        if (entity.level().getGameTime() - entry.tick() <= FRESH_TICKS) return entry.cfg();
        SERVER_CACHE.remove(entity);
        return null;
    }

    public static boolean hasWings(LivingEntity entity) {
        return get(entity) != null;
    }

    private static @Nullable WingsPower.Config resolveClient(LivingEntity entity) {
        List<WingsPower.Config> active = PowerLookup.active(entity, WINGS_ID, WingsPower.Config.class);
        return active.isEmpty() ? null : active.get(0);
    }
}
