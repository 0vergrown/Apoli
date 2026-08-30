package dev.overgrown.apoli.client.particle;

import dev.overgrown.apoli.Apoli;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class ParticleTextures {

    private static final String TEXTURES = "textures/";
    private static final String PARTICLE_DIR = "textures/particle/";
    private static final String PNG = ".png";

    private static final Map<ResourceLocation, ResourceLocation> RESOLVED = new HashMap<>();

    private ParticleTextures() {}

    public static synchronized void clearCache() {
        RESOLVED.clear();
    }

    public static synchronized ResourceLocation resolve(ResourceLocation declared) {
        ResourceLocation cached = RESOLVED.get(declared);
        if (cached != null) return cached;
        ResourceLocation found = search(declared);
        if (found == null) {
            Apoli.LOGGER.warn("[Apoli] Particle texture {} is not in any loaded resource pack — "
                + "expecting assets/{}/{}. The particle will draw as the missing-texture checker.",
                declared, declared.getNamespace(), declared.getPath());
            found = declared;
        }
        RESOLVED.put(declared, found);
        return found;
    }

    private static ResourceLocation search(ResourceLocation declared) {
        String path = declared.getPath();
        if (exists(declared)) return declared;
        for (String candidate : candidates(path)) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(declared.getNamespace(), candidate);
            if (exists(id)) return id;
        }
        return null;
    }

    private static String[] candidates(String path) {
        String withPng = path.endsWith(PNG) ? path : path + PNG;
        if (path.startsWith(TEXTURES)) return new String[]{withPng};
        return new String[]{withPng, PARTICLE_DIR + withPng, TEXTURES + withPng};
    }

    private static boolean exists(ResourceLocation id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
