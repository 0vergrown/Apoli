package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Function;

public final class DepthWritingRenderTypes extends RenderType {

    private static final Function<ResourceLocation, RenderType> EMISSIVE = Util.memoize(texture ->
        new DepthWritingRenderTypes("apoli_entity_translucent_emissive_depth", true, Optional.of(RenderType.outline(texture)),
            new TextureStateShard(texture, false, false), RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER,
            TRANSLUCENT_TRANSPARENCY, NO_CULL, OVERLAY));

    private static final Function<ResourceLocation, RenderType> EYES = Util.memoize(texture ->
        new DepthWritingRenderTypes("apoli_eyes_depth", false, Optional.empty(),
            new TextureStateShard(texture, false, false), RENDERTYPE_EYES_SHADER,
            ADDITIVE_TRANSPARENCY, CULL, NO_OVERLAY));

    private final Optional<RenderType> outline;

    private DepthWritingRenderTypes(String name, boolean affectsCrumbling, Optional<RenderType> outline,
                                    RenderStateShard texture, RenderStateShard shader, RenderStateShard transparency,
                                    RenderStateShard cull, RenderStateShard overlay) {
        this(name, affectsCrumbling, outline, new RenderStateShard[]{
            texture, shader, transparency, LEQUAL_DEPTH_TEST, cull, NO_LIGHTMAP, overlay, NO_LAYERING,
            MAIN_TARGET, DEFAULT_TEXTURING, COLOR_DEPTH_WRITE, NO_COLOR_LOGIC, DEFAULT_LINE
        });
    }

    private DepthWritingRenderTypes(String name, boolean affectsCrumbling, Optional<RenderType> outline,
                                    RenderStateShard[] shards) {
        super(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, affectsCrumbling, true,
            () -> {
                for (RenderStateShard shard : shards) shard.setupRenderState();
            },
            () -> {
                for (RenderStateShard shard : shards) shard.clearRenderState();
            });
        this.outline = outline;
    }

    public static RenderType emissive(ResourceLocation texture) {
        return EMISSIVE.apply(texture);
    }

    public static RenderType eyes(ResourceLocation texture) {
        return EYES.apply(texture);
    }

    @Override
    public Optional<RenderType> outline() {
        return this.outline;
    }
}
