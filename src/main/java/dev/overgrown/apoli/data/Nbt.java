package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

public record Nbt(CompoundTag tag) {
    private static final Codec<Nbt> OBJECT_CODEC = Codec.PASSTHROUGH.flatXmap(
        dyn -> {
            Tag t = dyn.convert(NbtOps.INSTANCE).getValue();
            if (t instanceof CompoundTag c) return DataResult.success(new Nbt(c));
            return DataResult.error(() -> "Expected NBT compound, got " + t.getType().getName());
        },
        nbt -> DataResult.success(new Dynamic<>(NbtOps.INSTANCE, nbt.tag()))
    );

    private static final Codec<Nbt> STRING_CODEC = Codec.STRING.comapFlatMap(
        s -> {
            try { return DataResult.success(new Nbt(TagParser.parseTag(s))); }
            catch (Exception e) { return DataResult.error(() -> "Invalid SNBT: " + e.getMessage()); }
        },
        nbt -> nbt.tag().toString()
    );

    public static final Codec<Nbt> CODEC = Codec.either(STRING_CODEC, OBJECT_CODEC).xmap(
        either -> either.map(java.util.function.Function.identity(), java.util.function.Function.identity()),
        nbt -> Either.right(nbt)
    );
}
