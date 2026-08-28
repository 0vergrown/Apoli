package dev.overgrown.apoli.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.overgrown.apoli.particle.ParticleBlend;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Environment(EnvType.CLIENT)
public final class ApoliParticleRenderTypes {

    private static final Map<ResourceLocation, ParticleRenderType> TRANSLUCENT = new HashMap<>();
    private static final Map<ResourceLocation, ParticleRenderType> ADDITIVE = new HashMap<>();
    private static final List<ParticleRenderType> ORDER = new ArrayList<>(4);

    private ApoliParticleRenderTypes() {}

    public static ParticleRenderType of(ResourceLocation texture, ParticleBlend blend) {
        Map<ResourceLocation, ParticleRenderType> cache = blend == ParticleBlend.ADDITIVE ? ADDITIVE : TRANSLUCENT;
        ParticleRenderType cached = cache.get(texture);
        if (cached != null) return cached;
        ParticleRenderType created = new Type(texture, blend == ParticleBlend.ADDITIVE);
        cache.put(texture, created);
        if (blend == ParticleBlend.ADDITIVE) ORDER.add(created);
        else ORDER.add(0, created);
        return created;
    }

    public static void render(Map<ParticleRenderType, Queue<Particle>> buckets, TextureManager textureManager,
                              Camera camera, float partialTick) {
        if (ORDER.isEmpty() || buckets.isEmpty()) return;
        boolean drew = false;
        for (int i = 0; i < ORDER.size(); i++) {
            ParticleRenderType type = ORDER.get(i);
            Queue<Particle> queue = buckets.get(type);
            if (queue == null || queue.isEmpty()) continue;
            RenderSystem.setShader(GameRenderer::getParticleShader);
            BufferBuilder builder = type.begin(Tesselator.getInstance(), textureManager);
            if (builder == null) continue;
            drew = true;
            for (Particle particle : queue) particle.render(builder, camera, partialTick);
            MeshData mesh = builder.build();
            if (mesh != null) BufferUploader.drawWithShader(mesh);
        }
        if (!drew) return;
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    private record Type(ResourceLocation texture, boolean additive) implements ParticleRenderType {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            if (additive) {
                RenderSystem.blendFuncSeparate(
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(true);
            }
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "apoli:custom[" + texture + (additive ? ",additive]" : "]");
        }
    }
}
