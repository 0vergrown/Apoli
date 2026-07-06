package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.data.RenderMode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class OverlayRenderTypes {
    private OverlayRenderTypes() {}

    public static RenderType forMode(RenderMode mode, ResourceLocation texture) {
        return switch (mode) {
            case TRANSLUCENT -> RenderType.entityTranslucent(texture);
            case TRANSLUCENT_CULL -> RenderType.entityTranslucentCull(texture);
            case CUTOUT -> RenderType.entityCutout(texture);
            case CUTOUT_NO_CULL -> RenderType.entityCutoutNoCull(texture);
            case SOLID -> RenderType.entitySolid(texture);
            case EMISSIVE -> RenderType.entityTranslucentEmissive(texture);
            case EYES -> RenderType.eyes(texture);
        };
    }
}
