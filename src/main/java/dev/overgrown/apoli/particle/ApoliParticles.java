package dev.overgrown.apoli.particle;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.Apoli;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class ApoliParticles {

    public static final ParticleType<CustomParticleOptions> CUSTOM = new ParticleType<>(false) {
        @Override
        public MapCodec<CustomParticleOptions> codec() {
            return CustomParticleOptions.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, CustomParticleOptions> streamCodec() {
            return CustomParticleOptions.STREAM_CODEC;
        }
    };

    private ApoliParticles() {}

    public static void register() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Apoli.id("custom"), CUSTOM);
    }
}
