package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.License;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Expression {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*:[a-zA-Z0-9_./-]+");

    static {
        try {
            License.iConfirmNonCommercialUse("Apoli — Minecraft mod (dev.overgrown.apoli)");
        } catch (Throwable ignored) {
        }
    }

    public static final Codec<Expression> CODEC = Codec.STRING.xmap(Expression::of, Expression::source);

    public static final Codec<Expression> INT_OR_EXPR = Codec.either(Codec.INT, Codec.STRING).xmap(
        either -> either.map(Expression::constant, Expression::of),
        expr -> expr.constantValue().isPresent() && expr.constantValue().getAsDouble() == (int) expr.constantValue().getAsDouble()
            ? Either.left((int) expr.constantValue().getAsDouble())
            : Either.right(expr.source())
    );

    public static final Codec<Expression> FLOAT_OR_EXPR = Codec.either(Codec.FLOAT, Codec.STRING).xmap(
        either -> either.map(f -> constant(f.doubleValue()), Expression::of),
        expr -> expr.constantValue().isPresent()
            ? Either.left((float) expr.constantValue().getAsDouble())
            : Either.right(expr.source())
    );

    private final String source;
    private final String rewritten;
    private final Map<String, String> sanitizedToOriginal;
    private final OptionalDouble constant;

    private Expression(String source, String rewritten, Map<String, String> sanitizedToOriginal, OptionalDouble constant) {
        this.source = source;
        this.rewritten = rewritten;
        this.sanitizedToOriginal = sanitizedToOriginal;
        this.constant = constant;
    }

    public static Expression of(String src) {
        if (src == null || src.isBlank()) {
            throw new IllegalArgumentException("Expression source must not be empty");
        }
        String trimmed = src.trim();
        try {
            double v = Double.parseDouble(trimmed);
            return new Expression(src, trimmed, Map.of(), OptionalDouble.of(v));
        } catch (NumberFormatException ignored) {
        }
        Map<String, String> originalToSanitized = new LinkedHashMap<>();
        Matcher m = RESOURCE_ID.matcher(src);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            String token = m.group();
            String sanitized = originalToSanitized.computeIfAbsent(token, Expression::sanitize);
            out.append(src, last, m.start()).append(sanitized);
            last = m.end();
        }
        out.append(src, last, src.length());
        Map<String, String> sanitizedToOriginal = new LinkedHashMap<>();
        originalToSanitized.forEach((orig, san) -> sanitizedToOriginal.put(san, orig));
        return new Expression(src, out.toString(), Map.copyOf(sanitizedToOriginal), OptionalDouble.empty());
    }

    public static Expression constant(double value) {
        String repr = value == (long) value ? Long.toString((long) value) : Double.toString(value);
        return new Expression(repr, repr, Map.of(), OptionalDouble.of(value));
    }

    private static String sanitize(String resourceId) {
        return "_r_" + resourceId.replace(':', '_').replace('/', '_').replace('.', '_').replace('-', '_');
    }

    public String source() {
        return source;
    }

    public OptionalDouble constantValue() {
        return constant;
    }

    public Set<String> referencedResources() {
        return sanitizedToOriginal.values().stream().collect(Collectors.toUnmodifiableSet());
    }

    public double eval(Map<String, Double> variables, Map<String, Double> resources) {
        if (constant.isPresent()) return constant.getAsDouble();
        Argument[] args = new Argument[variables.size() + sanitizedToOriginal.size()];
        int i = 0;
        for (Map.Entry<String, Double> e : variables.entrySet()) {
            args[i++] = new Argument(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, String> e : sanitizedToOriginal.entrySet()) {
            double v = resources.getOrDefault(e.getValue(), 0.0);
            args[i++] = new Argument(e.getKey(), v);
        }
        org.mariuszgromada.math.mxparser.Expression mx =
            new org.mariuszgromada.math.mxparser.Expression(rewritten, args);
        double result = mx.calculate();
        return Double.isNaN(result) || Double.isInfinite(result) ? 0.0 : result;
    }

    public int evalInt(Map<String, Double> variables, Map<String, Double> resources) {
        return (int) Math.round(eval(variables, resources));
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
