package dev.overgrown.apoli.mixin.loot;

import dev.overgrown.apoli.access.KeyedLootTable;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReloadableServerRegistries.Holder.class)
public abstract class ReloadableServerRegistriesHolderMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void apoli$keyLootTables(RegistryAccess.Frozen registries, CallbackInfo ci) {
        registries.registry(Registries.LOOT_TABLE).ifPresent(registry -> registry.entrySet().forEach(entry -> {
            LootTable table = entry.getValue();
            if (table != LootTable.EMPTY) ((KeyedLootTable) table).apoli$setKey(entry.getKey().location());
        }));
    }
}
