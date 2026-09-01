package dev.overgrown.apoli.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
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
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder builder = tesselator.getBuilder();
            type.begin(builder, textureManager);
            drew = true;
            for (Particle particle : queue) particle.render(builder, camera, partialTick);
            type.end(tesselator);
        }
        if (!drew) return;
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    private record Type(ResourceLocation texture, boolean additive) implements ParticleRenderType {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
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
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
        }

        @Override
        public String toString() {
            return "apoli:custom[" + texture + (additive ? ",additive]" : "]");
        }
    }
}
