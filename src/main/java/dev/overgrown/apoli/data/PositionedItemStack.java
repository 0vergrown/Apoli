package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.OptionalInt;

public record PositionedItemStack(ItemStack stack, OptionalInt slot, boolean lock) {

    private record Placement(Optional<Integer> slot, boolean lock) {}

    private static final MapCodec<Placement> PLACEMENT = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.INT.optionalFieldOf("slot").forGetter(Placement::slot),
        Codec.BOOL.optionalFieldOf("lock", false).forGetter(Placement::lock)
    ).apply(i, Placement::new));

    public static final Codec<PositionedItemStack> CODEC =
        Codec.mapPair(ItemStackData.MAP_CODEC, PLACEMENT).xmap(
            pair -> new PositionedItemStack(
                pair.getFirst().stack(),
                pair.getSecond().slot().map(OptionalInt::of).orElse(OptionalInt.empty()),
                pair.getSecond().lock()),
            positioned -> Pair.of(
                new ItemStackData(positioned.stack()),
                new Placement(positioned.slot().isPresent()
                    ? Optional.of(positioned.slot().getAsInt())
                    : Optional.empty(), positioned.lock()))
        ).codec();
}
