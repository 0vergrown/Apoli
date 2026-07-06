package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record Vector(float x, float y, float z) {
    public static final Vector ZERO = new Vector(0f, 0f, 0f);

    private static final Codec<Vector> LIST_CODEC = Codec.FLOAT.listOf().comapFlatMap(
        list -> list.size() == 3
            ? DataResult.success(new Vector(list.get(0), list.get(1), list.get(2)))
            : DataResult.error(() -> "Vector list must have exactly 3 elements, got " + list.size()),
        v -> List.of(v.x, v.y, v.z)
    );

    private static final Codec<Vector> OBJECT_CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(Vector::x),
        Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(Vector::y),
        Codec.FLOAT.optionalFieldOf("z", 0f).forGetter(Vector::z)
    ).apply(i, Vector::new));

    public static final Codec<Vector> CODEC = Codec.either(OBJECT_CODEC, LIST_CODEC).xmap(
        either -> either.map(java.util.function.Function.identity(), java.util.function.Function.identity()),
        v -> Either.left(v)
    );

    
    public static final Codec<Vector> SCALAR_OR_VECTOR = Codec.either(Codec.FLOAT, CODEC).xmap(
        either -> either.map(n -> new Vector(n, n, n), java.util.function.Function.identity()),
        v -> (v.x == v.y && v.y == v.z) ? Either.left(v.x) : Either.right(v)
    );

    public static Vector uniform(float value) { return new Vector(value, value, value); }

    public Vector add(Vector o) { return new Vector(x + o.x, y + o.y, z + o.z); }
    public Vector scale(float k) { return new Vector(x * k, y * k, z * k); }
    public double length() { return Math.sqrt(x * x + y * y + z * z); }
}
