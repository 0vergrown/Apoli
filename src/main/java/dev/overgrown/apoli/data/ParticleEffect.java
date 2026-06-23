package dev.overgrown.apoli.data;

import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ParticleEffect(ResourceLocation type, String params) {

    public static final ParticleEffect EMPTY = new ParticleEffect(new ResourceLocation("minecraft", "poof"), "");

    private static final Codec<ParticleEffect> STRING_CODEC = ResourceLocation.CODEC.xmap(
        rl -> new ParticleEffect(rl, ""),
        ParticleEffect::type
    );

    private static final Codec<ParticleEffect> OBJECT_CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceLocation.CODEC.fieldOf("type").forGetter(ParticleEffect::type),
        Codec.STRING.optionalFieldOf("params", "").forGetter(ParticleEffect::params)
    ).apply(i, ParticleEffect::new));

    public static final Codec<ParticleEffect> CODEC = Codec.either(STRING_CODEC, OBJECT_CODEC).xmap(
        either -> either.map(java.util.function.Function.identity(), java.util.function.Function.identity()),
        eff -> eff.params.isEmpty() ? Either.left(eff) : Either.right(eff)
    );

    public @Nullable ParticleOptions resolve(net.minecraft.world.level.Level level) {
        ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(type);
        if (particleType == null) return null;
        return readUnchecked(particleType, params);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable ParticleOptions readUnchecked(ParticleType<?> rawType, String params) {
        try {
            ParticleType type = rawType;
            String command = params.isEmpty() || Character.isWhitespace(params.charAt(0)) ? params : " " + params;
            StringReader reader = new StringReader(command);
            return type.getDeserializer().fromCommand(type, reader);
        } catch (Exception e) {
            return null;
        }
    }
}
