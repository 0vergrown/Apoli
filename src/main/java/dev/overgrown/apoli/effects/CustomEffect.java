package dev.overgrown.apoli.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.phys.Vec3;

import java.util.List;
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
                Codec.STRING.xmap(MobEffectCategory::valueOf, Enum::name).fieldOf("type").forGetter(CustomEffect::mobEffectCategory),
                Codec.STRING.optionalFieldOf("name").forGetter(CustomEffect::name)
        ).apply(instance, (powers, x, y, z, icon, pr, category, name) ->
                new CustomEffect(id, powers, new Vec3(x, y ,z), icon, pr, category, new CustomEffect(id, powers, new Vec3(x, y ,z), icon, pr, category, null, name).mobEffect(), name)));
    }

    public MobEffect mobEffect() {
        return new CustomMobEffect(this,
                (int)(color.x * 255) << 16
                | (int)(color.y * 255) << 8
                | (int)(color.z * 255));
    }

    public MobEffect getMobEffect() {
        return mobEffect;
    }
}
