package dev.overgrown.apoli.particle;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.Apoli;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ApoliParticles {

    public static final ParticleType<CustomParticleOptions> CUSTOM =
        new ParticleType<>(false, CustomParticleOptions.DESERIALIZER) {
            @Override
            public Codec<CustomParticleOptions> codec() {
                return CustomParticleOptions.CODEC;
            }
        };

    private ApoliParticles() {}

    public static void register() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Apoli.id("custom"), CUSTOM);
    }
}
