package dev.overgrown.apoli.mixin.packassets;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.packassets.client.ClientPackAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

// Offers the downloaded data pack assets to the client's resource pack list on every scan. Only the game's own
// resource pack repository is touched; data pack repositories share the class and are left alone.
@Mixin(PackRepository.class)
public abstract class PackRepositoryAssetsMixin {
    @ModifyReturnValue(method = "discoverAvailable", at = @At("RETURN"))
    private Map<String, Pack> apoli$addDataPackAssets(Map<String, Pack> original) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getResourcePackRepository() != (Object) this) return original;
        Pack pack = ClientPackAssets.profile();
        if (pack == null || original.containsKey(pack.getId())) return original;
        return ImmutableMap.<String, Pack>builder().putAll(original).put(pack.getId(), pack).build();
    }
}
