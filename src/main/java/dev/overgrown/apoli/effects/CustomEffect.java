package dev.overgrown.apoli.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record CustomEffect(
        ResourceLocation id,
        List<ResourceLocation> powers,
        Vec3 color,
        Optional<ResourceLocation> icon,
        int loadingPriority,
        MobEffectCategory mobEffectCategory,
        MobEffect mobEffect,
        Optional<String> name
    ) {

    public static Codec<CustomEffect> codec(ResourceLocation id) {
        return RecordCodecBuilder.create(instance -> instance.group(
                IdCodecs.ID.listOf().fieldOf("powers").forGetter(CustomEffect::powers),
                Codec.DOUBLE.optionalFieldOf("r", 0d).forGetter(effect -> effect.color.x),
                Codec.DOUBLE.optionalFieldOf("g", 0d).forGetter(effect -> effect.color.y),
                Codec.DOUBLE.optionalFieldOf("b", 0d).forGetter(effect -> effect.color.z),
                IdCodecs.ID.optionalFieldOf("icon").forGetter(CustomEffect::icon),
                Codec.INT.optionalFieldOf("loading_priority", 0).forGetter(CustomEffect::loadingPriority),
                CATEGORY_CODEC.fieldOf("type").forGetter(CustomEffect::mobEffectCategory),
                Codec.STRING.optionalFieldOf("name").forGetter(CustomEffect::name)
        ).apply(instance, (powers, x, y, z, icon, pr, category, name) ->
                CustomEffect.create(id, powers, new Vec3(x, y, z), icon, pr, category, name))
        );
    }

    private static final Codec<MobEffectCategory> CATEGORY_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(MobEffectCategory.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown effect type '" + name
                            + "', expected one of " + Arrays.toString(MobEffectCategory.values()));
                }
            },
            MobEffectCategory::name
    );

    public MobEffect mobEffect() {
        return new CustomMobEffect(this, colorInt(), name, id, icon);
    }

    public MobEffect getMobEffect() {
        return mobEffect;
    }

    public int colorInt() {
        return (int) (Math.round(color.x * 255) << 16
            | Math.round(color.y * 255) << 8
            | Math.round(color.z * 255));
    }

    public static CustomEffect create(ResourceLocation id, List<ResourceLocation> powers, Vec3 color,
                                      Optional<ResourceLocation> icon, int priority, MobEffectCategory category, Optional<String> name) {
        CustomEffect draft = new CustomEffect(id, powers, color, icon, priority, category, null, name);
        return new CustomEffect(id, powers, color, icon, priority, category, draft.mobEffect(), name);
    }
}
