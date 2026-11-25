package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.data.RenderMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ShaderAlpha {
    private static final int LEVELS = 32;
    private static final Map<ResourceLocation, ResourceLocation[]> BAKED = new HashMap<>();
    private static final Set<ResourceLocation> UNREADABLE = new HashSet<>();
    private static final @Nullable MethodHandle SHADER_PACK_IN_USE = findShaderPackCheck();

    private ShaderAlpha() {}

    public static ResourceLocation texture(ResourceLocation source, RenderMode mode, float alpha) {
        if (alpha >= 1.0F || !translucent(mode) || !shaderPackInUse()) return source;
        int level = Math.max(0, Math.round(alpha * LEVELS));
        if (level >= LEVELS) return source;
        ResourceLocation[] variants = BAKED.get(source);
        if (variants == null) {
            if (UNREADABLE.contains(source)) return source;
            variants = new ResourceLocation[LEVELS];
            BAKED.put(source, variants);
        }
        ResourceLocation baked = variants[level];
        if (baked != null) return baked;
        baked = bake(source, level);
        if (baked == null) {
            UNREADABLE.add(source);
            BAKED.remove(source);
            return source;
        }
        variants[level] = baked;
        return baked;
    }

    public static void clear() {
        if (BAKED.isEmpty() && UNREADABLE.isEmpty()) return;
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation[] variants : BAKED.values()) {
            for (ResourceLocation id : variants) {
                if (id != null) textures.release(id);
            }
        }
        BAKED.clear();
        UNREADABLE.clear();
    }

    private static boolean translucent(RenderMode mode) {
        return mode == RenderMode.TRANSLUCENT || mode == RenderMode.TRANSLUCENT_CULL || mode == RenderMode.EMISSIVE;
    }

    private static boolean shaderPackInUse() {
        MethodHandle check = SHADER_PACK_IN_USE;
        if (check == null) return false;
        try {
            return (boolean) check.invokeExact();
        } catch (Throwable e) {
            return false;
        }
    }

    private static @Nullable ResourceLocation bake(ResourceLocation source, int level) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(source);
        if (resource.isEmpty()) return null;
        NativeImage image;
        try (InputStream in = resource.get().open()) {
            image = NativeImage.read(in);
        } catch (IOException | RuntimeException e) {
            return null;
        }
        float scale = level / (float) LEVELS;
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getPixelRGBA(x, y);
                int scaled = Math.round((pixel >>> 24) * scale);
                image.setPixelRGBA(x, y, (pixel & 0x00FFFFFF) | (scaled << 24));
            }
        }
        ResourceLocation id = Apoli.id("baked_alpha/" + source.getNamespace() + "/" + source.getPath() + "_" + level);
        minecraft.getTextureManager().register(id, new DynamicTexture(image));
        return id;
    }

    private static @Nullable MethodHandle findShaderPackCheck() {
        if (!ModCompat.IRIS) return null;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return MethodHandles.publicLookup()
                .findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class))
                .bindTo(instance);
        } catch (Throwable e) {
            return null;
        }
    }
}
