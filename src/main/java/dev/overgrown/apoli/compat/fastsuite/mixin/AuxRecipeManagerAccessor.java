package dev.overgrown.apoli.compat.fastsuite.mixin;

import dev.shadowsoffire.fastsuite.AuxRecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = AuxRecipeManager.class, remap = false)
public interface AuxRecipeManagerAccessor {
    @Accessor("cachedRecipeListMap")
    Map<?, ?> apoli$cachedRecipeLists();
}
