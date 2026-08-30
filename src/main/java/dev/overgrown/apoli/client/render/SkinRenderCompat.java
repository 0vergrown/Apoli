package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.ModelColorPower;
import dev.overgrown.apoli.power.builtin.ModifyPlayerModelPower;
import dev.overgrown.apoli.power.builtin.PreventFeatureRenderPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class SkinRenderCompat {

    public static final String[] SKIN_LAYERS_3D = {"skin_layers_3d"};
    public static final String[] EARS = {"ears"};

    private SkinRenderCompat() {}

    public static boolean suppressed(@Nullable LivingEntity entity, String[] featureKeys) {
        if (entity == null) return false;
        Entity source = ClientDisguiseManager.powerSource(entity);
        if (PreventFeatureRenderPower.prevents(source, featureKeys, net.minecraft.client.Minecraft.getInstance().player)) return true;
        if (!(source instanceof LivingEntity living)) return false;
        return CustomModelRenderPower.replacesSkin(living)
            || ModifyPlayerModelPower.firstActiveModel(living) != null;
    }

    public static int tint(int color, @Nullable LivingEntity entity, @Nullable ModelPart part) {
        if (entity == null) return color;
        if (part != null && part.skipDraw) return color & 0x00FFFFFF;

        float[] whole = ModelColorPower.colorFor(ClientDisguiseManager.powerSource(entity));
        float[] partColor = part == null || !ModelColorState.isActive() ? null : ModelColorState.colorFor(part);
        if (whole == ModelColorPower.IDENTITY && partColor == null) return color;

        float r = whole[0], g = whole[1], b = whole[2], a = whole[3];
        if (partColor != null) {
            r *= partColor[0];
            g *= partColor[1];
            b *= partColor[2];
            a *= partColor[3];
        }
        return FastColor.ARGB32.color(
            scale(FastColor.ARGB32.alpha(color), a),
            scale(FastColor.ARGB32.red(color), r),
            scale(FastColor.ARGB32.green(color), g),
            scale(FastColor.ARGB32.blue(color), b));
    }

    public static int overlay(int overlay, @Nullable LivingEntity entity, @Nullable ModelPart part) {
        if (entity == null) return overlay;
        if (ModelColorPower.colorFor(ClientDisguiseManager.powerSource(entity))[4] < 1.0F) {
            float[] partColor = part == null || !ModelColorState.isActive() ? null : ModelColorState.colorFor(part);
            if (partColor == null || partColor[4] < 1.0F) return overlay;
        }
        return OverlayTexture.pack(15, overlay >> 16 & 0xFFFF);
    }

    public static float[] rgba(@Nullable LivingEntity entity) {
        if (entity == null) return ModelColorPower.IDENTITY;
        return ModelColorPower.colorFor(ClientDisguiseManager.powerSource(entity));
    }

    private static int scale(int channel, float factor) {
        return Math.min(255, Math.round(channel * factor));
    }
}
