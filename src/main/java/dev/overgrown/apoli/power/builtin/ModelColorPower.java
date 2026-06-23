package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class ModelColorPower extends PowerType<ModelColorPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("model_color");

    public record Config(float red, float green, float blue, float alpha) {}

    public static final float[] IDENTITY = new float[]{1f, 1f, 1f, 1f};

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("red", 1f).forGetter(Config::red),
            Codec.FLOAT.optionalFieldOf("green", 1f).forGetter(Config::green),
            Codec.FLOAT.optionalFieldOf("blue", 1f).forGetter(Config::blue),
            Codec.FLOAT.optionalFieldOf("alpha", 1f).forGetter(Config::alpha)
        ).apply(i, Config::new));
    }

    public static float[] colorFor(LivingEntity entity) {
        float[] rgba = new float[]{1f, 1f, 1f, 1f};
        boolean[] any = new boolean[]{false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            any[0] = true;
            rgba[0] *= cfg.red;
            rgba[1] *= cfg.green;
            rgba[2] *= cfg.blue;
            rgba[3] *= cfg.alpha;
        });
        return any[0] ? rgba : IDENTITY;
    }
}
