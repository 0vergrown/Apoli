package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum EquipmentSlot implements StringRepresentable {
    MAINHAND("mainhand", net.minecraft.world.entity.EquipmentSlot.MAINHAND),
    OFFHAND("offhand", net.minecraft.world.entity.EquipmentSlot.OFFHAND),
    HEAD("head", net.minecraft.world.entity.EquipmentSlot.HEAD),
    CHEST("chest", net.minecraft.world.entity.EquipmentSlot.CHEST),
    LEGS("legs", net.minecraft.world.entity.EquipmentSlot.LEGS),
    FEET("feet", net.minecraft.world.entity.EquipmentSlot.FEET);

    public static final Codec<EquipmentSlot> CODEC = StringRepresentable.fromEnum(EquipmentSlot::values);

    private final String name;
    private final net.minecraft.world.entity.EquipmentSlot vanilla;

    EquipmentSlot(String name, net.minecraft.world.entity.EquipmentSlot vanilla) {
        this.name = name;
        this.vanilla = vanilla;
    }

    public net.minecraft.world.entity.EquipmentSlot vanilla() {
        return vanilla;
    }

    public static EquipmentSlot fromVanilla(net.minecraft.world.entity.EquipmentSlot slot) {
        for (EquipmentSlot s : values()) {
            if (s.vanilla == slot) return s;
        }
        throw new IllegalArgumentException("No Apoli EquipmentSlot for vanilla " + slot);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
