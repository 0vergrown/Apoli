package dev.overgrown.apoli.condition.builtin.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.Entity;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class EntityNbtSnapshot {

    private EntityNbtSnapshot() {}

    private static final ThreadLocal<Boolean> SKIP_RECIPE_BOOK = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean isSkippingRecipeBook() {
        return SKIP_RECIPE_BOOK.get();
    }

    private static final class TickCache {
        long tick = Long.MIN_VALUE;
        final Map<Entity, CompoundTag> snapshots = new IdentityHashMap<>();
    }

    private static final ThreadLocal<TickCache> CACHE = ThreadLocal.withInitial(TickCache::new);

    public static CompoundTag of(Entity entity, CompoundTag expected) {
        Set<String> keys = expected.getAllKeys();
        if (canFastPath(keys)) {
            return fastSnapshot(entity, keys);
        }
        return cachedFullSnapshot(entity, expected);
    }

    private static boolean canFastPath(Set<String> keys) {
        for (String key : keys) {
            if (!"Tags".equals(key)) {
                return false;
            }
        }
        return true;
    }

    private static CompoundTag fastSnapshot(Entity entity, Set<String> keys) {
        CompoundTag actual = new CompoundTag();
        if (keys.contains("Tags")) {
            Set<String> tags = entity.getTags();
            if (!tags.isEmpty()) {
                ListTag list = new ListTag();
                for (String tag : tags) {
                    list.add(StringTag.valueOf(tag));
                }
                actual.put("Tags", list);
            }
        }
        return actual;
    }

    private static CompoundTag cachedFullSnapshot(Entity entity, CompoundTag expected) {
        if (expected.contains("recipeBook")) {
            return serialize(entity, false);
        }
        long now = entity.level().getGameTime();
        TickCache cache = CACHE.get();
        if (cache.tick != now) {
            cache.snapshots.clear();
            cache.tick = now;
        }
        CompoundTag cached = cache.snapshots.get(entity);
        if (cached == null) {
            cached = serialize(entity, true);
            cache.snapshots.put(entity, cached);
        }
        return cached;
    }

    private static CompoundTag serialize(Entity entity, boolean skipRecipeBook) {
        CompoundTag actual = new CompoundTag();
        if (skipRecipeBook) {
            SKIP_RECIPE_BOOK.set(Boolean.TRUE);
        }
        try {
            entity.saveWithoutId(actual);
        } finally {
            if (skipRecipeBook) {
                SKIP_RECIPE_BOOK.set(Boolean.FALSE);
            }
        }
        return actual;
    }
}
