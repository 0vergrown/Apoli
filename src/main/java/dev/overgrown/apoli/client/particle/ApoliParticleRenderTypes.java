package dev.overgrown.apoli.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.overgrown.apoli.particle.ParticleBlend;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ApoliParticleRenderTypes {

    private static final Map<ResourceLocation, ParticleRenderType> TRANSLUCENT = new HashMap<>();
    private static final Map<ResourceLocation, ParticleRenderType> ADDITIVE = new HashMap<>();

    private ApoliParticleRenderTypes() {}

    public static ParticleRenderType of(ResourceLocation texture, ParticleBlend blend) {
        Map<ResourceLocation, ParticleRenderType> cache = blend == ParticleBlend.ADDITIVE ? ADDITIVE : TRANSLUCENT;
        ParticleRenderType cached = cache.get(texture);
        if (cached != null) return cached;
        ParticleRenderType created = new Type(texture, blend == ParticleBlend.ADDITIVE);
        cache.put(texture, created);
        return created;
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
