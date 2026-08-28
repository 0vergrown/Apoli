package dev.overgrown.apoli.particle;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.Apoli;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ApoliParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, Apoli.MOD_ID);

    public static final Supplier<ParticleType<CustomParticleOptions>> CUSTOM =
        PARTICLE_TYPES.register("custom", () -> new ParticleType<CustomParticleOptions>(false) {
            @Override
            public MapCodec<CustomParticleOptions> codec() {
                return CustomParticleOptions.CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, CustomParticleOptions> streamCodec() {
                return CustomParticleOptions.STREAM_CODEC;
            }
        });

    private ApoliParticles() {}

    public static void register(IEventBus modBus) {
        PARTICLE_TYPES.register(modBus);
    }
}
