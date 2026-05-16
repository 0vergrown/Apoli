package dev.overgrown.apoli.mixin.keybinding;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes the private static {@code CATEGORY_SORT_ORDER} field on
 * {@link KeyMapping} so {@link dev.overgrown.apoli.client.DynamicKeyMappingManager}
 * can insert new categories at any time without going through Fabric's
 * registration helper (which is locked once {@code Options} has finished
 * constructing).
 */
@Mixin(KeyMapping.class)
public interface KeyMappingCategoryAccessor {
    @Accessor("CATEGORY_SORT_ORDER")
    static Map<String, Integer> apoli$getCategorySortOrder() {
        throw new AssertionError("Mixin not applied");
    }
}
