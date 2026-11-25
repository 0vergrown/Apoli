package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.expr.ExprMemo;
import dev.overgrown.apoli.data.expr.ExprParseException;
import dev.overgrown.apoli.data.expr.ExprParser;
import dev.overgrown.apoli.data.expr.ExprVars;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public record ExprVector(Expression x, Expression y, Expression z) {

    public static final ExprVector ZERO = uniform(0.0f);

    private static final String X_SUFFIX = ").x";
    private static final String Y_SUFFIX = ").y";
    private static final String Z_SUFFIX = ").z";

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

    private static final Codec<ExprVector> SHAPED_CODEC = Codec.either(OBJECT_CODEC, LIST_CODEC).xmap(
        either -> either.map(Function.identity(), Function.identity()),
        Either::left
    );

    private static final Codec<ExprVector> EXPRESSION_CODEC = Codec.STRING.comapFlatMap(
        ExprVector::tryParse,
        v -> {
            String source = v.expressionSource();
            return source != null ? source : "vec(" + v.x.source() + ", " + v.y.source() + ", " + v.z.source() + ")";
        }
    );

    public static final Codec<ExprVector> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ExprVector, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getStringValue(input).result().isPresent()
                ? EXPRESSION_CODEC.decode(ops, input)
                : SHAPED_CODEC.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(ExprVector input, DynamicOps<T> ops, T prefix) {
            return input.expressionSource() != null
                ? EXPRESSION_CODEC.encode(input, ops, prefix)
                : SHAPED_CODEC.encode(input, ops, prefix);
        }
    };

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

    public static ExprVector parse(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Vector expression must not be empty");
        }
        try {
            ExprParser.VectorResult result = ExprParser.parseVector(source, ExprVars::resolve);
            return new ExprVector(
                Expression.component("(" + source + X_SUFFIX, result.x(), result.needsContainer(), result.needsPeer(), result.usesMemo()),
                Expression.component("(" + source + Y_SUFFIX, result.y(), result.needsContainer(), result.needsPeer(), result.usesMemo()),
                Expression.component("(" + source + Z_SUFFIX, result.z(), result.needsContainer(), result.needsPeer(), result.usesMemo()));
        } catch (ExprParseException e) {
            throw new IllegalArgumentException("Invalid vector expression \"" + source + "\": " + e.getMessage());
        }
    }

    private static DataResult<ExprVector> tryParse(String source) {
        try {
            return DataResult.success(parse(source));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }

    public @Nullable String expressionSource() {
        String xs = x.source();
        if (!xs.startsWith("(") || !xs.endsWith(X_SUFFIX)) return null;
        String inner = xs.substring(1, xs.length() - X_SUFFIX.length());
        if (!y.source().equals("(" + inner + Y_SUFFIX) || !z.source().equals("(" + inner + Z_SUFFIX)) return null;
        return inner;
    }

    public boolean isUniform() {
        return x.equals(y) && y.equals(z);
    }

    public boolean isConstant() {
        return x.constantValue().isPresent() && y.constantValue().isPresent() && z.constantValue().isPresent();
    }

    private boolean usesMemo() {
        return x.usesMemo() || y.usesMemo() || z.usesMemo();
    }

    public Vector resolve(@Nullable Entity actor) {
        if (!usesMemo()) return new Vector((float) x.eval(actor), (float) y.eval(actor), (float) z.eval(actor));
        ExprMemo memo = ExprMemo.enter();
        try {
            return new Vector((float) x.eval(actor), (float) y.eval(actor), (float) z.eval(actor));
        } finally {
            memo.exit();
        }
    }

    public Vec3 resolveVec3(@Nullable Entity actor) {
        if (!usesMemo()) return new Vec3(x.eval(actor), y.eval(actor), z.eval(actor));
        ExprMemo memo = ExprMemo.enter();
        try {
            return new Vec3(x.eval(actor), y.eval(actor), z.eval(actor));
        } finally {
            memo.exit();
        }
    }
}
