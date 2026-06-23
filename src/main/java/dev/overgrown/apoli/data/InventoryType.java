package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum InventoryType implements StringRepresentable {
    INVENTORY("inventory"),
    POWER("power");

    public static final Codec<InventoryType> CODEC = StringRepresentable.fromEnum(InventoryType::values);

    private final String name;
    InventoryType(String n) { this.name = n; }

    @Override public String getSerializedName() { return name; }
}
