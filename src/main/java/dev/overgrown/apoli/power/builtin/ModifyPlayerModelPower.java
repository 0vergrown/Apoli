package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.data.ModelAnimation;
import dev.overgrown.apoli.data.TextureRef;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ModifyPlayerModelPower extends PowerType<ModifyPlayerModelPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_player_model");
    public static final ResourceLocation VANILLA_MODEL = Apoli.id("vanilla");

    public record Config(
        ResourceLocation model,
        Optional<ResourceLocation> texture,
        Optional<ResourceLocation> modelTexture,
        Optional<ModelAnimation> animations
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("model").forGetter(Config::model),
            TextureRef.ID_CODEC.optionalFieldOf("texture_location").forGetter(Config::texture),
            TextureRef.ID_CODEC.optionalFieldOf("model_texture_location").forGetter(Config::modelTexture),
            LoggedOptionalField.of("animations", ModelAnimation.CODEC).forGetter(Config::animations)
        ).apply(i, Config::new));
    }

    @Nullable
    public static Config firstActive(@Nullable Entity entity) {
        return PowerLookup.firstActive(entity, CANONICAL, Config.class);
    }

    @Nullable
    public static ResourceLocation firstActiveModel(@Nullable Entity entity) {
        Config config = firstActive(entity);
        return config == null ? null : config.model();
    }

    @Nullable
    public static ResourceLocation firstActiveTexture(@Nullable Entity entity) {
        Config config = firstActive(entity);
        return config == null ? null : config.texture().orElse(null);
    }

    public static boolean replacesAppearance(@Nullable Entity entity) {
        Config config = firstActive(entity);
        return config != null && (config.texture().isPresent() || !VANILLA_MODEL.equals(config.model()));
    }

    @Nullable
    public static ResourceLocation firstActiveModelTexture(@Nullable Entity entity) {
        Config config = firstActive(entity);
        return config == null ? null : config.modelTexture().orElse(null);
    }

    @Nullable
    public static ModelAnimation firstActiveAnimations(@Nullable Entity entity) {
        Config config = firstActive(entity);
        return config == null ? null : config.animations().orElse(null);
    }
}
