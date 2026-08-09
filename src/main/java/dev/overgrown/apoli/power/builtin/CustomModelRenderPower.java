package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.data.RenderMode;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CustomModelRenderPower extends PowerType<CustomModelRenderPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("custom_model_render");

    public enum Mode implements StringRepresentable {
        TEXTURE("texture"),
        GEOMETRY("geometry");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Config(
        Mode mode,
        Optional<ResourceLocation> wideTexture,
        Optional<ResourceLocation> slimTexture,
        Optional<ResourceLocation> model,
        Optional<ResourceLocation> texture,
        boolean renderAsOverlay,
        boolean hideCape,
        List<EquipmentSlot> hiddenSlots,
        RenderMode renderType,
        List<String> bodyParts,
        float red,
        float green,
        float blue,
        float alpha,
        boolean showFirstPerson,
        float scale
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
    }

    public record GeometryRender(
        ResourceLocation model,
        ResourceLocation texture,
        RenderMode mode,
        List<String> bodyParts,
        float red,
        float green,
        float blue,
        float alpha,
        boolean showFirstPerson,
        float scale,
        boolean renderAsOverlay
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            Mode.CODEC.optionalFieldOf("mode", Mode.TEXTURE).forGetter(Config::mode),
            ResourceLocation.CODEC.optionalFieldOf("wide_texture_location").forGetter(Config::wideTexture),
            ResourceLocation.CODEC.optionalFieldOf("slim_texture_location").forGetter(Config::slimTexture),
            ResourceLocation.CODEC.optionalFieldOf("model_location").forGetter(Config::model),
            ResourceLocation.CODEC.optionalFieldOf("texture_location").forGetter(Config::texture),
            Codec.BOOL.optionalFieldOf("render_as_overlay", false).forGetter(Config::renderAsOverlay),
            Codec.BOOL.optionalFieldOf("hide_cape", false).forGetter(Config::hideCape),
            EquipmentSlot.CODEC.listOf().optionalFieldOf("hidden_slots", List.of()).forGetter(Config::hiddenSlots),
            RenderMode.CODEC.optionalFieldOf("render_type", RenderMode.TRANSLUCENT).forGetter(Config::renderType),
            ModelParts.PART_LIST_CODEC.optionalFieldOf("body_parts", List.of()).forGetter(Config::bodyParts),
            Codec.FLOAT.optionalFieldOf("red", 1.0F).forGetter(Config::red),
            Codec.FLOAT.optionalFieldOf("green", 1.0F).forGetter(Config::green),
            Codec.FLOAT.optionalFieldOf("blue", 1.0F).forGetter(Config::blue),
            Codec.FLOAT.optionalFieldOf("alpha", 1.0F).forGetter(Config::alpha),
            Codec.BOOL.optionalFieldOf("show_first_person", false).forGetter(Config::showFirstPerson),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(Config::scale)
        ).apply(instance, Config::new));
    }

    private static boolean hiddenByEquipment(LivingEntity entity, List<EquipmentSlot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            if (!entity.getItemBySlot(slots.get(i).vanilla()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Config firstReplace(@Nullable LivingEntity entity) {
        Config[] found = new Config[1];
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (found[0] != null || cfg.mode() != Mode.TEXTURE || cfg.renderAsOverlay()) {
                return;
            }
            if (cfg.wideTexture().isEmpty() || hiddenByEquipment(entity, cfg.hiddenSlots())) {
                return;
            }
            found[0] = cfg;
        });
        return found[0];
    }

    public static boolean replacesSkin(@Nullable LivingEntity entity) {
        boolean[] hit = {false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (hit[0] || cfg.renderAsOverlay() || hiddenByEquipment(entity, cfg.hiddenSlots())) {
                return;
            }
            hit[0] = cfg.mode() == Mode.TEXTURE
                ? cfg.wideTexture().isPresent()
                : cfg.model().isPresent() && cfg.texture().isPresent();
        });
        return hit[0];
    }

    public static boolean shouldHideCape(@Nullable LivingEntity entity) {
        boolean[] hide = {false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (cfg.hideCape()) {
                hide[0] = true;
            }
        });
        return hide[0];
    }

    public static List<ResolvedLayer> collectTextureOverlays(@Nullable LivingEntity entity) {
        List<ResolvedLayer> out = new ArrayList<>();
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (cfg.mode() != Mode.TEXTURE || !cfg.renderAsOverlay()) {
                return;
            }
            ResourceLocation wide = cfg.wide();
            if (wide == null || hiddenByEquipment(entity, cfg.hiddenSlots())) {
                return;
            }
            ResourceLocation slim = cfg.slim();
            out.add(new ResolvedLayer(wide, slim != null ? slim : wide, cfg.renderType(), cfg.bodyParts(),
                cfg.red(), cfg.green(), cfg.blue(), cfg.alpha(), cfg.showFirstPerson(), cfg.scale()));
        });
        return out;
    }

    public static List<GeometryRender> collectGeometry(@Nullable LivingEntity entity) {
        List<GeometryRender> out = new ArrayList<>();
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (cfg.mode() != Mode.GEOMETRY || cfg.model().isEmpty() || cfg.texture().isEmpty()) {
                return;
            }
            if (hiddenByEquipment(entity, cfg.hiddenSlots())) {
                return;
            }
            out.add(new GeometryRender(cfg.model().get(), cfg.texture().get(), cfg.renderType(), cfg.bodyParts(),
                cfg.red(), cfg.green(), cfg.blue(), cfg.alpha(), cfg.showFirstPerson(), cfg.scale(), cfg.renderAsOverlay()));
        });
        return out;
    }
}
