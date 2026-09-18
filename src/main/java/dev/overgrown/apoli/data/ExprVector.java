package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public record ExprVector(Expression x, Expression y, Expression z) {

    public static final ExprVector ZERO = uniform(0.0f);

    private static final Codec<ExprVector> LIST_CODEC = Expression.FLOAT_OR_EXPR.listOf().comapFlatMap(
        list -> list.size() == 3
            ? DataResult.success(new ExprVector(list.get(0), list.get(1), list.get(2)))
            : DataResult.error(() -> "Vector list must have exactly 3 elements, got " + list.size()),
        v -> List.of(v.x, v.y, v.z)
    );

    private static final Codec<ExprVector> OBJECT_CODEC = RecordCodecBuilder.create(i -> i.group(
        Expression.FLOAT_OR_EXPR.optionalFieldOf("x", Expression.constant(0.0)).forGetter(ExprVector::x),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("y", Expression.constant(0.0)).forGetter(ExprVector::y),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("z", Expression.constant(0.0)).forGetter(ExprVector::z)
    ).apply(i, ExprVector::new));

    public static final Codec<ExprVector> CODEC = Codec.either(OBJECT_CODEC, LIST_CODEC).xmap(
        either -> either.map(Function.identity(), Function.identity()),
        Either::left
    );

    public static final Codec<ExprVector> SCALAR_OR_VECTOR =
        Codec.either(Expression.FLOAT_OR_EXPR, CODEC).xmap(
            either -> either.map(ExprVector::uniform, Function.identity()),
            v -> v.isUniform() ? Either.left(v.x) : Either.right(v)
        );

    public static ExprVector uniform(Expression value) {
        return new ExprVector(value, value, value);
    }

    public static ExprVector uniform(float value) {
        return uniform(Expression.constant(value));
    }

    public static ExprVector of(float x, float y, float z) {
        return new ExprVector(Expression.constant(x), Expression.constant(y), Expression.constant(z));
    }

    public boolean isUniform() {
        return x.equals(y) && y.equals(z);
    }

    public boolean isConstant() {
        return x.constantValue().isPresent() && y.constantValue().isPresent() && z.constantValue().isPresent();
    }

    public Vector resolve(@Nullable Entity actor) {
        return new Vector((float) x.eval(actor), (float) y.eval(actor), (float) z.eval(actor));
    }
}
