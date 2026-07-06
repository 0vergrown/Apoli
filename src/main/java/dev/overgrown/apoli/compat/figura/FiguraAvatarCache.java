package dev.overgrown.apoli.compat.figura;

import dev.overgrown.apoli.Apoli;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class FiguraAvatarCache {
    private FiguraAvatarCache() {}

    private static final Map<ResourceLocation, Optional<CompoundTag>> CACHE = new HashMap<>();
    private static final Set<ResourceLocation> WARNED = new HashSet<>();

    @Nullable
    public static CompoundTag get(ResourceLocation modelId) {
        return CACHE.computeIfAbsent(modelId, FiguraAvatarCache::load).orElse(null);
    }

    public static ResourceLocation toResourcePath(ResourceLocation modelId) {
        return new ResourceLocation(modelId.getNamespace(),
            "figura_avatars/" + modelId.getPath() + ".nbt");
    }

    private static Optional<CompoundTag> load(ResourceLocation modelId) {
        ResourceLocation file = toResourcePath(modelId);
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            if (WARNED.add(modelId)) {
                Apoli.LOGGER.warn("modify_player_model: no Figura avatar found at assets/{}/{} for model id {}",
                    file.getNamespace(), file.getPath(), modelId);
            }
            return Optional.empty();
        }
        try (InputStream in = resource.get().open()) {
            return Optional.of(NbtIo.readCompressed(in));
        } catch (Exception e) {
            if (WARNED.add(modelId)) {
                Apoli.LOGGER.error("modify_player_model: failed to read Figura avatar {} for model id {}",
                    file, modelId, e);
            }
            return Optional.empty();
        }
    }

    public static void clear() {
        CACHE.clear();
        WARNED.clear();
    }
}
