package dev.overgrown.apoli.particle;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.data.ColorCodecs;
import dev.overgrown.apoli.data.Easing;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record CustomParticleOptions(
    ResourceLocation texture,
    int lifetime,
    int lifetimeVariation,
    float size,
    float sizeVariation,
    Optional<Float> endSize,
    int color,
    Optional<Integer> endColor,
    float gravity,
    float friction,
    float roll,
    float rollSpeed,
    int frames,
    int frameTime,
    Optional<Boolean> loopFrames,
    boolean physics,
    boolean emissive,
    ParticleBlend blend,
    ParticleFacing facing,
    Easing easing,
    ParticleFrameLayout frameLayout
) implements ParticleOptions {

    private static final int FLAG_LOOP_VALUE = 1;
    private static final int FLAG_PHYSICS = 2;
    private static final int FLAG_EMISSIVE = 4;
    private static final int FLAG_ADDITIVE = 8;
    private static final int FLAG_FACING_VERTICAL = 16;
    private static final int FLAG_LOOP_SET = 32;
    private static final int LAYOUT_SHIFT = 6;
    private static final int LAYOUT_MASK = 3;

    private record Extra(ParticleBlend blend, ParticleFacing facing, Easing easing,
                        ParticleFrameLayout frameLayout, float sizeVariation) {}

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
        Codec.INT.optionalFieldOf("frames", 0).forGetter(CustomParticleOptions::frames),
        Codec.INT.optionalFieldOf("frame_time", 0).forGetter(CustomParticleOptions::frameTime),
        Codec.BOOL.optionalFieldOf("loop_frames").forGetter(CustomParticleOptions::loopFrames),
        Codec.BOOL.optionalFieldOf("physics", false).forGetter(CustomParticleOptions::physics),
        Codec.BOOL.optionalFieldOf("emissive", false).forGetter(CustomParticleOptions::emissive)
    ).apply(instance, (texture, lifetime, lifetimeVariation, size, endSize, color, endColor, gravity, friction,
                       roll, rollSpeed, frames, frameTime, loopFrames, physics, emissive) ->
        new CustomParticleOptions(texture, lifetime, lifetimeVariation, size, 0.0F, endSize, color, endColor, gravity,
            friction, roll, rollSpeed, frames, frameTime, loopFrames, physics, emissive,
            ParticleBlend.TRANSLUCENT, ParticleFacing.CAMERA, Easing.LINEAR, ParticleFrameLayout.AUTO)));

    private static final MapCodec<Extra> EXTRA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ParticleBlend.CODEC.optionalFieldOf("blend", ParticleBlend.TRANSLUCENT).forGetter(Extra::blend),
        ParticleFacing.CODEC.optionalFieldOf("facing", ParticleFacing.CAMERA).forGetter(Extra::facing),
        Easing.CODEC.optionalFieldOf("easing", Easing.LINEAR).forGetter(Extra::easing),
        ParticleFrameLayout.CODEC.optionalFieldOf("frame_layout", ParticleFrameLayout.AUTO).forGetter(Extra::frameLayout),
        Codec.FLOAT.optionalFieldOf("size_variation", 0.0F).forGetter(Extra::sizeVariation)
    ).apply(instance, Extra::new));

    public static final MapCodec<CustomParticleOptions> CODEC = Codec.mapPair(BODY_CODEC, EXTRA_CODEC).xmap(
        pair -> pair.getFirst().withExtra(pair.getSecond()),
        options -> Pair.of(options, new Extra(options.blend(), options.facing(), options.easing(),
            options.frameLayout(), options.sizeVariation())));

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomParticleOptions> STREAM_CODEC = StreamCodec.of(
        (buf, options) -> options.write(buf),
        CustomParticleOptions::read);

    private CustomParticleOptions withExtra(Extra extra) {
        return new CustomParticleOptions(texture, lifetime, lifetimeVariation, size, extra.sizeVariation(),
            endSize, color, endColor, gravity, friction, roll, rollSpeed, frames, frameTime, loopFrames,
            physics, emissive, extra.blend(), extra.facing(), extra.easing(), extra.frameLayout());
    }

    public float endSizeOr() {
        return endSize.orElse(size);
    }

    public int endColorOr() {
        return endColor.orElse(color);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(texture);
        buf.writeVarInt(lifetime);
        buf.writeVarInt(lifetimeVariation);
        buf.writeFloat(size);
        buf.writeFloat(sizeVariation);
        buf.writeFloat(endSizeOr());
        buf.writeInt(color);
        buf.writeInt(endColorOr());
        buf.writeFloat(gravity);
        buf.writeFloat(friction);
        buf.writeFloat(roll);
        buf.writeFloat(rollSpeed);
        buf.writeVarInt(frames);
        buf.writeVarInt(frameTime);
        int flags = (loopFrames.orElse(false) ? FLAG_LOOP_VALUE : 0)
            | (physics ? FLAG_PHYSICS : 0)
            | (emissive ? FLAG_EMISSIVE : 0)
            | (blend == ParticleBlend.ADDITIVE ? FLAG_ADDITIVE : 0)
            | (facing == ParticleFacing.VERTICAL ? FLAG_FACING_VERTICAL : 0)
            | (loopFrames.isPresent() ? FLAG_LOOP_SET : 0)
            | (frameLayout.ordinal() << LAYOUT_SHIFT);
        buf.writeByte(flags);
        buf.writeByte(easing.ordinal());
    }

    private static CustomParticleOptions read(RegistryFriendlyByteBuf buf) {
        ResourceLocation texture = buf.readResourceLocation();
        int lifetime = buf.readVarInt();
        int lifetimeVariation = buf.readVarInt();
        float size = buf.readFloat();
        float sizeVariation = buf.readFloat();
        float endSize = buf.readFloat();
        int color = buf.readInt();
        int endColor = buf.readInt();
        float gravity = buf.readFloat();
        float friction = buf.readFloat();
        float roll = buf.readFloat();
        float rollSpeed = buf.readFloat();
        int frames = buf.readVarInt();
        int frameTime = buf.readVarInt();
        int flags = buf.readByte() & 0xFF;
        Easing[] easings = Easing.values();
        int easingIndex = buf.readByte();
        Easing easing = easingIndex >= 0 && easingIndex < easings.length ? easings[easingIndex] : Easing.LINEAR;
        ParticleFrameLayout[] layouts = ParticleFrameLayout.values();
        ParticleFrameLayout layout = layouts[(flags >> LAYOUT_SHIFT) & LAYOUT_MASK];
        Optional<Boolean> loopFrames = (flags & FLAG_LOOP_SET) != 0
            ? Optional.of((flags & FLAG_LOOP_VALUE) != 0)
            : Optional.empty();
        return new CustomParticleOptions(texture, lifetime, lifetimeVariation, size, sizeVariation,
            Optional.of(endSize), color, Optional.of(endColor), gravity, friction, roll, rollSpeed, frames, frameTime,
            loopFrames, (flags & FLAG_PHYSICS) != 0, (flags & FLAG_EMISSIVE) != 0,
            (flags & FLAG_ADDITIVE) != 0 ? ParticleBlend.ADDITIVE : ParticleBlend.TRANSLUCENT,
            (flags & FLAG_FACING_VERTICAL) != 0 ? ParticleFacing.VERTICAL : ParticleFacing.CAMERA,
            easing, layout);
    }

    @Override
    public ParticleType<CustomParticleOptions> getType() {
        return ApoliParticles.CUSTOM.get();
    }
}
