package dev.overgrown.apoli.mixin.effects;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.effects.CustomEffectRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(RegistrySyncManager.class)
public class FabricRegistrySyncCustomEffectMixin {
    @ModifyReturnValue(method = "createAndPopulateRegistryMap", at = @At("RETURN"))
    @Nullable
    private static Map<ResourceLocation, Object2IntMap<ResourceLocation>> apoli$filterCustomEffects(@Nullable Map<ResourceLocation, Object2IntMap<ResourceLocation>> original) {
        var custom_effects = CustomEffectRegistry.byId.keySet();

        if (original == null) {
            return null;
        }

        custom_effects.forEach(id ->
                original.get(Registries.MOB_EFFECT.location()).removeInt(id)
        );

        return original;
    }
}
