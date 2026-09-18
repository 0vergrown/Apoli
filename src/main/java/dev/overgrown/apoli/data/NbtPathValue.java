package dev.overgrown.apoli.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class NbtPathValue {
    private NbtPathValue() {}

    public static final int FIRST = 0;
    public static final int SUM = 1;
    public static final int MAX = 2;
    public static final int MIN = 3;
    public static final int COUNT = 4;

    private static final NbtPathArgument ARGUMENT = new NbtPathArgument();

    public static int aggregatorOf(String name) {
        return switch (name) {
            case "first" -> FIRST;
            case "sum" -> SUM;
            case "max" -> MAX;
            case "min" -> MIN;
            case "count", "size" -> COUNT;
            default -> -1;
        };
    }

    @Nullable
    public static NbtPathArgument.NbtPath parse(String path) {
        try {
            StringReader reader = new StringReader(path);
            NbtPathArgument.NbtPath parsed = ARGUMENT.parse(reader);
            return reader.canRead() ? null : parsed;
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    public static double read(@Nullable Tag root, NbtPathArgument.NbtPath path, int aggregator) {
        if (root == null) return 0;
        int size = path.countMatching(root);
        if (aggregator == COUNT || size == 0) return size;
        List<Tag> found;
        try {
            found = path.get(root);
        } catch (CommandSyntaxException e) {
            return 0;
        }
        if (aggregator == FIRST) return valueOf(found.get(0));
        double result = aggregator == MAX ? Double.NEGATIVE_INFINITY
            : aggregator == MIN ? Double.POSITIVE_INFINITY : 0;
        for (int i = 0; i < found.size(); i++) {
            double value = valueOf(found.get(i));
            switch (aggregator) {
                case SUM -> result += value;
                case MAX -> result = Math.max(result, value);
                case MIN -> result = Math.min(result, value);
                default -> { }
            }
        }
        return result;
    }

    public static double valueOf(Tag tag) {
        if (tag instanceof NumericTag numeric) return numeric.getAsDouble();
        if (tag instanceof StringTag string) return string.getAsString().length();
        if (tag instanceof CollectionTag<?> collection) return collection.size();
        if (tag instanceof CompoundTag compound) return compound.size();
        return 0;
    }
}
