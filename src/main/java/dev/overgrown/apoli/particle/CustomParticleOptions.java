package dev.overgrown.apoli.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.data.ColorCodecs;
import dev.overgrown.apoli.data.Easing;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record CustomParticleOptions(
    ResourceLocation texture,
    int lifetime,
    int lifetimeVariation,
    float size,
    Optional<Float> endSize,
    int color,
    Optional<Integer> endColor,
    float gravity,
    float friction,
    float roll,
    float rollSpeed,
    int frames,
    int frameTime,
    boolean loopFrames,
    boolean physics,
    boolean emissive,
    ParticleBlend blend,
    ParticleFacing facing,
    Easing easing
) implements ParticleOptions {

    private record Look(ParticleBlend blend, ParticleFacing facing, Easing easing) {}

    private static final MapCodec<CustomParticleOptions> BODY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        IdCodecs.ID.fieldOf("texture").forGetter(CustomParticleOptions::texture),
        Codec.INT.optionalFieldOf("lifetime", 20).forGetter(CustomParticleOptions::lifetime),
        Codec.INT.optionalFieldOf("lifetime_variation", 0).forGetter(CustomParticleOptions::lifetimeVariation),
        Codec.FLOAT.optionalFieldOf("size", 0.2F).forGetter(CustomParticleOptions::size),
        Codec.FLOAT.optionalFieldOf("end_size").forGetter(CustomParticleOptions::endSize),
        ColorCodecs.ARGB.optionalFieldOf("color", 0xFFFFFFFF).forGetter(CustomParticleOptions::color),
        ColorCodecs.ARGB.optionalFieldOf("end_color").forGetter(CustomParticleOptions::endColor),
        Codec.FLOAT.optionalFieldOf("gravity", 0.0F).forGetter(CustomParticleOptions::gravity),
        Codec.FLOAT.optionalFieldOf("friction", 0.98F).forGetter(CustomParticleOptions::friction),
        Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(CustomParticleOptions::roll),
        Codec.FLOAT.optionalFieldOf("roll_speed", 0.0F).forGetter(CustomParticleOptions::rollSpeed),
        Codec.INT.optionalFieldOf("frames", 1).forGetter(CustomParticleOptions::frames),
        Codec.INT.optionalFieldOf("frame_time", 0).forGetter(CustomParticleOptions::frameTime),
        Codec.BOOL.optionalFieldOf("loop_frames", false).forGetter(CustomParticleOptions::loopFrames),
        Codec.BOOL.optionalFieldOf("physics", false).forGetter(CustomParticleOptions::physics),
        Codec.BOOL.optionalFieldOf("emissive", false).forGetter(CustomParticleOptions::emissive)
    ).apply(instance, (texture, lifetime, lifetimeVariation, size, endSize, color, endColor, gravity, friction,
                       roll, rollSpeed, frames, frameTime, loopFrames, physics, emissive) ->
        new CustomParticleOptions(texture, lifetime, lifetimeVariation, size, endSize, color, endColor, gravity,
            friction, roll, rollSpeed, frames, frameTime, loopFrames, physics, emissive,
            ParticleBlend.TRANSLUCENT, ParticleFacing.CAMERA, Easing.LINEAR)));

    private static final MapCodec<Look> LOOK_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ParticleBlend.CODEC.optionalFieldOf("blend", ParticleBlend.TRANSLUCENT).forGetter(Look::blend),
        ParticleFacing.CODEC.optionalFieldOf("facing", ParticleFacing.CAMERA).forGetter(Look::facing),
        Easing.CODEC.optionalFieldOf("easing", Easing.LINEAR).forGetter(Look::easing)
    ).apply(instance, Look::new));

    public static final Codec<CustomParticleOptions> CODEC = Codec.mapPair(BODY_CODEC, LOOK_CODEC).xmap(
        pair -> pair.getFirst().withLook(pair.getSecond()),
        options -> Pair.of(options, new Look(options.blend(), options.facing(), options.easing()))).codec();

    public static final Deserializer<CustomParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public CustomParticleOptions fromCommand(ParticleType<CustomParticleOptions> type, StringReader reader)
            throws CommandSyntaxException {
            reader.expect(' ');
            return of(ResourceLocation.read(reader));
        }

        @Override
        public CustomParticleOptions fromNetwork(ParticleType<CustomParticleOptions> type, FriendlyByteBuf buf) {
            return read(buf);
        }
    };

    public static CustomParticleOptions of(ResourceLocation texture) {
        return new CustomParticleOptions(texture, 20, 0, 0.2F, Optional.empty(), 0xFFFFFFFF, Optional.empty(),
            0.0F, 0.98F, 0.0F, 0.0F, 1, 0, false, false, false,
            ParticleBlend.TRANSLUCENT, ParticleFacing.CAMERA, Easing.LINEAR);
    }

    private CustomParticleOptions withLook(Look look) {
        return new CustomParticleOptions(texture, lifetime, lifetimeVariation, size, endSize, color, endColor,
            gravity, friction, roll, rollSpeed, frames, frameTime, loopFrames, physics, emissive,
            look.blend(), look.facing(), look.easing());
    }

    public float endSizeOr() {
        return endSize.orElse(size);
    }

    public int endColorOr() {
        return endColor.orElse(color);
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(texture);
        buf.writeVarInt(lifetime);
        buf.writeVarInt(lifetimeVariation);
        buf.writeFloat(size);
        buf.writeFloat(endSizeOr());
        buf.writeInt(color);
        buf.writeInt(endColorOr());
        buf.writeFloat(gravity);
        buf.writeFloat(friction);
        buf.writeFloat(roll);
        buf.writeFloat(rollSpeed);
        buf.writeVarInt(frames);
        buf.writeVarInt(frameTime);
        int flags = (loopFrames ? 1 : 0) | (physics ? 2 : 0) | (emissive ? 4 : 0)
            | (blend == ParticleBlend.ADDITIVE ? 8 : 0) | (facing == ParticleFacing.VERTICAL ? 16 : 0);
        buf.writeByte(flags);
        buf.writeByte(easing.ordinal());
    }

    private static CustomParticleOptions read(FriendlyByteBuf buf) {
        ResourceLocation texture = buf.readResourceLocation();
        int lifetime = buf.readVarInt();
        int lifetimeVariation = buf.readVarInt();
        float size = buf.readFloat();
        float endSize = buf.readFloat();
        int color = buf.readInt();
        int endColor = buf.readInt();
        float gravity = buf.readFloat();
        float friction = buf.readFloat();
        float roll = buf.readFloat();
        float rollSpeed = buf.readFloat();
        int frames = buf.readVarInt();
        int frameTime = buf.readVarInt();
        int flags = buf.readByte();
        Easing[] easings = Easing.values();
        int easingIndex = buf.readByte();
        Easing easing = easingIndex >= 0 && easingIndex < easings.length ? easings[easingIndex] : Easing.LINEAR;
        return new CustomParticleOptions(texture, lifetime, lifetimeVariation, size, Optional.of(endSize),
            color, Optional.of(endColor), gravity, friction, roll, rollSpeed, frames, frameTime,
            (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0,
            (flags & 8) != 0 ? ParticleBlend.ADDITIVE : ParticleBlend.TRANSLUCENT,
            (flags & 16) != 0 ? ParticleFacing.VERTICAL : ParticleFacing.CAMERA,
            easing);
    }

    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(getType()) + " " + texture;
    }

    @Override
    public ParticleType<CustomParticleOptions> getType() {
        return ApoliParticles.CUSTOM;
    }
}
