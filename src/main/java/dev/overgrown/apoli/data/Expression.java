package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.data.expr.ExprNode;
import dev.overgrown.apoli.data.expr.ExprNodes;
import dev.overgrown.apoli.data.expr.ExprParseException;
import dev.overgrown.apoli.data.expr.ExprParser;
import dev.overgrown.apoli.data.expr.ExprVars;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;

public final class Expression {

    public static final Codec<Expression> CODEC = Codec.STRING.comapFlatMap(Expression::tryOf, Expression::source);

    public static final Codec<Expression> INT_OR_EXPR = Codec.either(Codec.INT, CODEC).xmap(
        either -> either.map(Expression::constant, expr -> expr),
        expr -> expr.constantValue().isPresent() && expr.constantValue().getAsDouble() == (int) expr.constantValue().getAsDouble()
            ? Either.left((int) expr.constantValue().getAsDouble())
            : Either.right(expr)
    );

    public static final Codec<Expression> FLOAT_OR_EXPR = Codec.either(Codec.FLOAT, CODEC).xmap(
        either -> either.map(f -> constant(f.doubleValue()), expr -> expr),
        expr -> expr.constantValue().isPresent()
            ? Either.left((float) expr.constantValue().getAsDouble())
            : Either.right(expr)
    );

    public static final Codec<Expression> DOUBLE_OR_EXPR = Codec.either(Codec.DOUBLE, CODEC).xmap(
        either -> either.map(Expression::constant, expr -> expr),
        expr -> expr.constantValue().isPresent()
            ? Either.left(expr.constantValue().getAsDouble())
            : Either.right(expr)
    );

    private final String source;
    private final ExprNode root;
    private final boolean isConst;
    private final double constVal;
    private final boolean needsContainer;

    private Expression(String source, ExprNode root, boolean needsContainer) {
        this.source = source;
        this.root = root;
        this.needsContainer = needsContainer;
        if (root instanceof ExprNodes.Const c) {
            this.isConst = true;
            this.constVal = c.v();
        } else {
            this.isConst = false;
            this.constVal = 0.0;
        }
    }

    public static Expression of(String src) {
        if (src == null || src.isBlank()) {
            throw new IllegalArgumentException("Expression source must not be empty");
        }
        try {
            ExprParser.Result result = ExprParser.parse(src, ExprVars::resolve);
            return new Expression(src, result.root(), result.needsContainer());
        } catch (ExprParseException e) {
            throw new IllegalArgumentException("Invalid expression \"" + src + "\": " + e.getMessage());
        }
    }

    private static DataResult<Expression> tryOf(String src) {
        try {
            return DataResult.success(of(src));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }

    public static Expression constant(double value) {
        String repr = value == (long) value ? Long.toString((long) value) : Double.toString(value);
        return new Expression(repr, new ExprNodes.Const(value), false);
    }

    public String source() {
        return source;
    }

    public OptionalDouble constantValue() {
        return isConst ? OptionalDouble.of(constVal) : OptionalDouble.empty();
    }

    public boolean needsContainer() {
        return needsContainer;
    }

    public double eval(@Nullable Entity entity) {
        if (isConst) return sanitize(constVal);
        return evalFull(entity, autoContainer(entity), null, 0.0);
    }

    public double eval(@Nullable Entity entity, double value) {
        if (isConst) return sanitize(constVal);
        return evalFull(entity, autoContainer(entity), null, value);
    }

    public double evalWith(@Nullable Entity entity, @Nullable PowerContainer container, double value) {
        if (isConst) return sanitize(constVal);
        return evalFull(entity, container, null, value);
    }

    public double eval(@Nullable Level level) {
        if (isConst) return sanitize(constVal);
        return evalFull(null, null, level, 0.0);
    }

    public int evalInt(@Nullable Entity entity) {
        return (int) Math.round(eval(entity));
    }

    public int evalInt(@Nullable Entity entity, double value) {
        return (int) Math.round(eval(entity, value));
    }

    public int evalIntWith(@Nullable Entity entity, @Nullable PowerContainer container, double value) {
        return (int) Math.round(evalWith(entity, container, value));
    }

    private double evalFull(@Nullable Entity entity, @Nullable PowerContainer container, @Nullable Level level, double value) {
        return sanitize(root.eval(entity, container, level, value));
    }

    private static double sanitize(double result) {
        return Double.isNaN(result) || Double.isInfinite(result) ? 0.0 : result;
    }

    private @Nullable PowerContainer autoContainer(@Nullable Entity entity) {
        return needsContainer && entity != null ? PowerContainer.of(entity) : null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Expression e && e.source.equals(this.source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @Override
    public String toString() {
        return "Expression[" + source + "]";
    }
}
