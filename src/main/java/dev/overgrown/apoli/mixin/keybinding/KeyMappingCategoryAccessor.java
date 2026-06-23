package dev.overgrown.apoli.mixin.keybinding;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingCategoryAccessor {
    @Accessor("CATEGORY_SORT_ORDER")
    static Map<String, Integer> apoli$getCategorySortOrder() {
        throw new AssertionError("Mixin not applied");
    }
}
