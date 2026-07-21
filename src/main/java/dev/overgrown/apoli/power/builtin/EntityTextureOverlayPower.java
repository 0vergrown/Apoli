package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.data.OverlayLayer;
import dev.overgrown.apoli.data.RenderMode;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EntityTextureOverlayPower extends PowerType<EntityTextureOverlayPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("entity_texture_overlay");

    public record Config(
        Optional<ResourceLocation> wideTexture,
        Optional<ResourceLocation> slimTexture,
        boolean showFirstPerson,
        boolean renderAsOverlay,
        RenderMode renderType,
        List<String> bodyParts,
        float red,
        float green,
        float blue,
        float alpha,
        boolean hideCape,
        float scale,
        List<OverlayLayer> layers
    ) {
        @Nullable
        public ResourceLocation wide() {
            return wideTexture.orElse(null);
        }

        @Nullable
        public ResourceLocation slim() {
            return slimTexture.or(() -> wideTexture).orElse(null);
        }
    }

    public record ResolvedLayer(
        ResourceLocation wide,
        ResourceLocation slim,
        RenderMode mode,
        List<String> bodyParts,
        float red,
        float green,
        float blue,
        float alpha,
        boolean showFirstPerson,
        float scale
    ) {
        public ResourceLocation texture(boolean slimModel) {
            return slimModel ? slim : wide;
        }

        public boolean wholeModel() {
            return bodyParts.isEmpty();
        }

        public boolean scaled() {
            return scale != 1.0F;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("wide_texture_location").forGetter(Config::wideTexture),
            ResourceLocation.CODEC.optionalFieldOf("slim_texture_location").forGetter(Config::slimTexture),
            Codec.BOOL.optionalFieldOf("show_first_person", false).forGetter(Config::showFirstPerson),
            Codec.BOOL.optionalFieldOf("render_as_overlay", false).forGetter(Config::renderAsOverlay),
            RenderMode.CODEC.optionalFieldOf("render_type", RenderMode.TRANSLUCENT).forGetter(Config::renderType),
            ModelParts.PART_LIST_CODEC.optionalFieldOf("body_parts", List.of()).forGetter(Config::bodyParts),
            Codec.FLOAT.optionalFieldOf("red", 1.0F).forGetter(Config::red),
            Codec.FLOAT.optionalFieldOf("green", 1.0F).forGetter(Config::green),
            Codec.FLOAT.optionalFieldOf("blue", 1.0F).forGetter(Config::blue),
            Codec.FLOAT.optionalFieldOf("alpha", 1.0F).forGetter(Config::alpha),
            Codec.BOOL.optionalFieldOf("hide_cape", false).forGetter(Config::hideCape),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(Config::scale),
            OverlayLayer.CODEC.listOf().optionalFieldOf("layers", List.of()).forGetter(Config::layers)
        ).apply(instance, Config::new));
    }

    @Nullable
    public static Config firstReplace(@Nullable LivingEntity entity) {
        Config[] found = new Config[1];
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (found[0] == null && !cfg.renderAsOverlay() && cfg.wideTexture().isPresent()) {
                found[0] = cfg;
            }
        });
        return found[0];
    }

    public static boolean shouldHideCape(@Nullable LivingEntity entity) {
        boolean[] hide = {false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (cfg.hideCape()) hide[0] = true;
        });
        return hide[0];
    }

    public static List<ResolvedLayer> collectLayers(@Nullable LivingEntity entity) {
        List<ResolvedLayer> out = new ArrayList<>();
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            ResourceLocation baseWide = cfg.wide();
            ResourceLocation baseSlim = cfg.slim();
            if (cfg.renderAsOverlay() && baseWide != null) {
                out.add(new ResolvedLayer(
                    baseWide, baseSlim != null ? baseSlim : baseWide, cfg.renderType(), cfg.bodyParts(),
                    cfg.red(), cfg.green(), cfg.blue(), cfg.alpha(), cfg.showFirstPerson(), cfg.scale()));
            }
            for (OverlayLayer layer : cfg.layers()) {
                ResourceLocation wide = layer.wide(baseWide);
                if (wide == null) continue;
                ResourceLocation slim = layer.slim(baseSlim != null ? baseSlim : baseWide);
                if (slim == null) slim = wide;
                out.add(new ResolvedLayer(
                    wide, slim, layer.renderType(), layer.bodyParts(),
                    layer.red(), layer.green(), layer.blue(), layer.alpha(), layer.showFirstPerson(), layer.scale()));
            }
        });
        return out;
    }
}
