package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record OverlayLayer(
    Optional<ResourceLocation> wideTexture,
    Optional<ResourceLocation> slimTexture,
    Optional<ResourceLocation> texture,
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
    public ResourceLocation wide(@Nullable ResourceLocation base) {
        return wideTexture.or(() -> texture).orElse(base);
    }

    @Nullable
    public ResourceLocation slim(@Nullable ResourceLocation base) {
        return slimTexture.or(() -> texture).orElse(base);
    }

    public boolean wholeModel() {
        return bodyParts.isEmpty();
    }

    public static final Codec<OverlayLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        IdCodecs.ID.optionalFieldOf("wide_texture_location").forGetter(OverlayLayer::wideTexture),
        IdCodecs.ID.optionalFieldOf("slim_texture_location").forGetter(OverlayLayer::slimTexture),
        IdCodecs.ID.optionalFieldOf("texture_location").forGetter(OverlayLayer::texture),
        RenderMode.CODEC.optionalFieldOf("render_type", RenderMode.TRANSLUCENT).forGetter(OverlayLayer::renderType),
        ModelParts.PART_LIST_CODEC.optionalFieldOf("body_parts", List.of()).forGetter(OverlayLayer::bodyParts),
        Codec.FLOAT.optionalFieldOf("red", 1.0F).forGetter(OverlayLayer::red),
        Codec.FLOAT.optionalFieldOf("green", 1.0F).forGetter(OverlayLayer::green),
        Codec.FLOAT.optionalFieldOf("blue", 1.0F).forGetter(OverlayLayer::blue),
        Codec.FLOAT.optionalFieldOf("alpha", 1.0F).forGetter(OverlayLayer::alpha),
        Codec.BOOL.optionalFieldOf("show_first_person", false).forGetter(OverlayLayer::showFirstPerson),
        Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(OverlayLayer::scale)
    ).apply(instance, OverlayLayer::new));
}
