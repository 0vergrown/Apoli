package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.List;

public enum InventoryType implements StringRepresentable {
    INVENTORY("inventory"),
    POWER("power");

    public static final Codec<InventoryType> CODEC = StringRepresentable.fromEnum(InventoryType::values);
    public static final Codec<List<InventoryType>> LIST_OR_SINGLE = Codec.either(CODEC, CODEC.listOf()).xmap(
        either -> either.map(List::of, list -> list),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    private final String name;
    InventoryType(String n) { this.name = n; }

    @Override public String getSerializedName() { return name; }
}
