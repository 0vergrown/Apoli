package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.data.RenderMode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class OverlayRenderTypes {
    private OverlayRenderTypes() {}

    public static RenderType forMode(RenderMode mode, ResourceLocation texture) {
        return forMode(mode, texture, 0.0F, 0.0F);
    }

    public static RenderType forMode(RenderMode mode, ResourceLocation texture, float ageInTicks, float scrollSpeed) {
        return switch (mode) {
            case TRANSLUCENT -> RenderType.entityTranslucent(texture);
            case TRANSLUCENT_CULL -> RenderType.entityTranslucentCull(texture);
            case CUTOUT -> RenderType.entityCutout(texture);
            case CUTOUT_NO_CULL -> RenderType.entityCutoutNoCull(texture);
            case SOLID -> RenderType.entitySolid(texture);
            case EMISSIVE -> RenderType.entityTranslucentEmissive(texture);
            case EYES -> RenderType.eyes(texture);
            case ENERGY_SWIRL -> energySwirl(texture, ageInTicks, scrollSpeed);
        };
    }

    private static RenderType energySwirl(ResourceLocation texture, float ageInTicks, float scrollSpeed) {
        if (scrollSpeed == 0.0F) {
            return RenderType.energySwirl(texture, 0.0F, 0.0F);
        }
        float u = (Mth.cos(ageInTicks * 0.02F) * 3.0F) % 1.0F;
        float v = (ageInTicks * scrollSpeed) % 1.0F;
        return RenderType.energySwirl(texture, u, v);
    }
}
