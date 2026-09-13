package dev.overgrown.apoli.particle;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.data.ColorCodecs;
import dev.overgrown.apoli.data.Easing;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record CustomParticleOptions(
    ResourceLocation texture,
    Expression lifetimeExpr,
    Expression lifetimeVariationExpr,
    Expression sizeExpr,
    Expression sizeVariationExpr,
    Optional<Expression> endSizeExpr,
    int color,
    Optional<Integer> endColor,
    Expression gravityExpr,
    Expression frictionExpr,
    Expression roll,
    Expression rollSpeed,
    Expression framesExpr,
    Expression frameTimeExpr,
    Optional<Boolean> loopFrames,
    boolean physics,
    boolean emissive,
    ParticleBlend blend,
    ParticleFacing facing,
    Easing easing,
    ParticleFrameLayout frameLayout,
    Expression colorVariationExpr,
    Expression hueVariationExpr,
    boolean alphaBleed
) implements ParticleOptions {

    private static final Expression NO_ROLL = Expression.constant(0.0);
    private static final Expression ZERO = Expression.constant(0.0);
    private static final Expression DEFAULT_LIFETIME = Expression.constant(20.0);
    private static final Expression DEFAULT_SIZE = Expression.constant(0.2);
    private static final Expression DEFAULT_FRICTION = Expression.constant(0.98);
    private static final int MAX_ROLL_SOURCE = 256;

    private static final Codec<Expression> ROLL_CODEC = Expression.FLOAT_OR_EXPR.flatXmap(
        expression -> expression.source().length() <= MAX_ROLL_SOURCE
            ? com.mojang.serialization.DataResult.success(expression)
            : com.mojang.serialization.DataResult.error(
                () -> "roll expression must be at most " + MAX_ROLL_SOURCE + " characters"),
        com.mojang.serialization.DataResult::success);

    private static final int FLAG_LOOP_VALUE = 1;
    private static final int FLAG_PHYSICS = 2;
    private static final int FLAG_EMISSIVE = 4;
    private static final int FLAG_ADDITIVE = 8;
    private static final int FLAG_FACING_VERTICAL = 16;
    private static final int FLAG_LOOP_SET = 32;
    private static final int LAYOUT_SHIFT = 6;
    private static final int LAYOUT_MASK = 3;
    private static final int FLAG_ALPHA_BLEED = 256;

    private record Extra(ParticleBlend blend, ParticleFacing facing, Easing easing,
                         ParticleFrameLayout frameLayout, Expression sizeVariation,
                         Expression colorVariation, Expression hueVariation, boolean alphaBleed) {}

    private static final MapCodec<CustomParticleOptions> BODY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        IdCodecs.ID.fieldOf("texture").forGetter(CustomParticleOptions::texture),
        Expression.INT_OR_EXPR.optionalFieldOf("lifetime", DEFAULT_LIFETIME).forGetter(CustomParticleOptions::lifetimeExpr),
        Expression.INT_OR_EXPR.optionalFieldOf("lifetime_variation", ZERO).forGetter(CustomParticleOptions::lifetimeVariationExpr),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("size", DEFAULT_SIZE).forGetter(CustomParticleOptions::sizeExpr),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("end_size").forGetter(CustomParticleOptions::endSizeExpr),
        ColorCodecs.ARGB.optionalFieldOf("color", 0xFFFFFFFF).forGetter(CustomParticleOptions::color),
        ColorCodecs.ARGB.optionalFieldOf("end_color").forGetter(CustomParticleOptions::endColor),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("gravity", ZERO).forGetter(CustomParticleOptions::gravityExpr),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("friction", DEFAULT_FRICTION).forGetter(CustomParticleOptions::frictionExpr),
        ROLL_CODEC.optionalFieldOf("roll", NO_ROLL).forGetter(CustomParticleOptions::roll),
        ROLL_CODEC.optionalFieldOf("roll_speed", NO_ROLL).forGetter(CustomParticleOptions::rollSpeed),
        Expression.INT_OR_EXPR.optionalFieldOf("frames", ZERO).forGetter(CustomParticleOptions::framesExpr),
        Expression.INT_OR_EXPR.optionalFieldOf("frame_time", ZERO).forGetter(CustomParticleOptions::frameTimeExpr),
        Codec.BOOL.optionalFieldOf("loop_frames").forGetter(CustomParticleOptions::loopFrames),
        Codec.BOOL.optionalFieldOf("physics", false).forGetter(CustomParticleOptions::physics),
        Codec.BOOL.optionalFieldOf("emissive", false).forGetter(CustomParticleOptions::emissive)
    ).apply(instance, (texture, lifetime, lifetimeVariation, size, endSize, color, endColor, gravity, friction,
                       roll, rollSpeed, frames, frameTime, loopFrames, physics, emissive) ->
        new CustomParticleOptions(texture, lifetime, lifetimeVariation, size, ZERO, endSize, color, endColor, gravity,
            friction, roll, rollSpeed, frames, frameTime, loopFrames, physics, emissive,
            ParticleBlend.TRANSLUCENT, ParticleFacing.CAMERA, Easing.LINEAR, ParticleFrameLayout.AUTO, ZERO, ZERO,
            false)));

    private static final MapCodec<Extra> EXTRA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ParticleBlend.CODEC.optionalFieldOf("blend", ParticleBlend.TRANSLUCENT).forGetter(Extra::blend),
        ParticleFacing.CODEC.optionalFieldOf("facing", ParticleFacing.CAMERA).forGetter(Extra::facing),
        Easing.CODEC.optionalFieldOf("easing", Easing.LINEAR).forGetter(Extra::easing),
        ParticleFrameLayout.CODEC.optionalFieldOf("frame_layout", ParticleFrameLayout.AUTO).forGetter(Extra::frameLayout),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("size_variation", ZERO).forGetter(Extra::sizeVariation),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("color_variation", ZERO).forGetter(Extra::colorVariation),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("hue_variation", ZERO).forGetter(Extra::hueVariation),
        Codec.BOOL.optionalFieldOf("alpha_bleed", false).forGetter(Extra::alphaBleed)
    ).apply(instance, Extra::new));

    public static final MapCodec<CustomParticleOptions> CODEC = Codec.mapPair(BODY_CODEC, EXTRA_CODEC).xmap(
        pair -> pair.getFirst().withExtra(pair.getSecond()),
        options -> Pair.of(options, new Extra(options.blend(), options.facing(), options.easing(),
            options.frameLayout(), options.sizeVariationExpr(), options.colorVariationExpr(),
            options.hueVariationExpr(), options.alphaBleed())));

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomParticleOptions> STREAM_CODEC = StreamCodec.of(
        (buf, options) -> options.write(buf),
        CustomParticleOptions::read);

    private CustomParticleOptions withExtra(Extra extra) {
        return new CustomParticleOptions(texture, lifetimeExpr, lifetimeVariationExpr, sizeExpr, extra.sizeVariation(),
            endSizeExpr, color, endColor, gravityExpr, frictionExpr, roll, rollSpeed, framesExpr, frameTimeExpr,
            loopFrames, physics, emissive, extra.blend(), extra.facing(), extra.easing(), extra.frameLayout(),
            extra.colorVariation(), extra.hueVariation(), extra.alphaBleed());
    }

    public int lifetime() {
        return lifetimeExpr.evalInt((Entity) null);
    }

    public int lifetimeVariation() {
        return lifetimeVariationExpr.evalInt((Entity) null);
    }

    public float size() {
        return (float) sizeExpr.eval((Entity) null);
    }

    public float sizeVariation() {
        return (float) sizeVariationExpr.eval((Entity) null);
    }

    public Optional<Float> endSize() {
        return endSizeExpr.map(expression -> (float) expression.eval((Entity) null));
    }

    public float gravity() {
        return (float) gravityExpr.eval((Entity) null);
    }

    public float friction() {
        return (float) frictionExpr.eval((Entity) null);
    }

    public int frames() {
        return framesExpr.evalInt((Entity) null);
    }

    public int frameTime() {
        return frameTimeExpr.evalInt((Entity) null);
    }

    public float colorVariation() {
        return (float) colorVariationExpr.eval((Entity) null);
    }

    public float hueVariation() {
        return (float) hueVariationExpr.eval((Entity) null);
    }

    public float endSizeOr() {
        return endSizeExpr.isPresent() ? (float) endSizeExpr.get().eval((Entity) null) : size();
    }

    public int endColorOr() {
        return endColor.orElse(color);
    }

    public boolean dynamic() {
        return varies(lifetimeExpr) || varies(lifetimeVariationExpr) || varies(sizeExpr) || varies(sizeVariationExpr)
            || (endSizeExpr.isPresent() && varies(endSizeExpr.get())) || varies(gravityExpr) || varies(frictionExpr)
            || varies(framesExpr) || varies(frameTimeExpr) || varies(colorVariationExpr) || varies(hueVariationExpr);
    }

    public CustomParticleOptions bake(@Nullable Entity actor) {
        return new CustomParticleOptions(texture, bake(lifetimeExpr, actor), bake(lifetimeVariationExpr, actor),
            bake(sizeExpr, actor), bake(sizeVariationExpr, actor), endSizeExpr.map(e -> bake(e, actor)), color,
            endColor, bake(gravityExpr, actor), bake(frictionExpr, actor), roll, rollSpeed, bake(framesExpr, actor),
            bake(frameTimeExpr, actor), loopFrames, physics, emissive, blend, facing, easing, frameLayout,
            bake(colorVariationExpr, actor), bake(hueVariationExpr, actor), alphaBleed);
    }

    private static boolean varies(Expression expression) {
        return expression.constantValue().isEmpty();
    }

    private static Expression bake(Expression expression, @Nullable Entity actor) {
        return expression.constantValue().isPresent() ? expression : Expression.constant(expression.eval(actor));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(texture);
        buf.writeVarInt(Math.max(0, lifetime()));
        buf.writeVarInt(Math.max(0, lifetimeVariation()));
        buf.writeFloat(size());
        buf.writeFloat(sizeVariation());
        buf.writeFloat(endSizeOr());
        buf.writeInt(color);
        buf.writeInt(endColorOr());
        buf.writeFloat(gravity());
        buf.writeFloat(friction());
        buf.writeUtf(roll.source(), MAX_ROLL_SOURCE);
        buf.writeUtf(rollSpeed.source(), MAX_ROLL_SOURCE);
        buf.writeVarInt(Math.max(0, frames()));
        buf.writeVarInt(Math.max(0, frameTime()));
        int flags = (loopFrames.orElse(false) ? FLAG_LOOP_VALUE : 0)
            | (physics ? FLAG_PHYSICS : 0)
            | (emissive ? FLAG_EMISSIVE : 0)
            | (blend == ParticleBlend.ADDITIVE ? FLAG_ADDITIVE : 0)
            | (facing == ParticleFacing.VERTICAL ? FLAG_FACING_VERTICAL : 0)
            | (loopFrames.isPresent() ? FLAG_LOOP_SET : 0)
            | (alphaBleed ? FLAG_ALPHA_BLEED : 0)
            | (frameLayout.ordinal() << LAYOUT_SHIFT);
        buf.writeVarInt(flags);
        buf.writeByte(easing.ordinal());
        buf.writeFloat(colorVariation());
        buf.writeFloat(hueVariation());
    }

    private static CustomParticleOptions read(RegistryFriendlyByteBuf buf) {
        ResourceLocation texture = buf.readResourceLocation();
        Expression lifetime = Expression.constant(buf.readVarInt());
        Expression lifetimeVariation = Expression.constant(buf.readVarInt());
        Expression size = Expression.constant(buf.readFloat());
        Expression sizeVariation = Expression.constant(buf.readFloat());
        Expression endSize = Expression.constant(buf.readFloat());
        int color = buf.readInt();
        int endColor = buf.readInt();
        Expression gravity = Expression.constant(buf.readFloat());
        Expression friction = Expression.constant(buf.readFloat());
        Expression roll = Expression.cached(buf.readUtf(MAX_ROLL_SOURCE));
        Expression rollSpeed = Expression.cached(buf.readUtf(MAX_ROLL_SOURCE));
        Expression frames = Expression.constant(buf.readVarInt());
        Expression frameTime = Expression.constant(buf.readVarInt());
        int flags = buf.readVarInt();
        Easing[] easings = Easing.values();
        int easingIndex = buf.readByte();
        Easing easing = easingIndex >= 0 && easingIndex < easings.length ? easings[easingIndex] : Easing.LINEAR;
        Expression colorVariation = Expression.constant(buf.readFloat());
        Expression hueVariation = Expression.constant(buf.readFloat());
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
            easing, layout, colorVariation, hueVariation, (flags & FLAG_ALPHA_BLEED) != 0);
    }

    @Override
    public ParticleType<CustomParticleOptions> getType() {
        return ApoliParticles.CUSTOM.get();
    }
}
