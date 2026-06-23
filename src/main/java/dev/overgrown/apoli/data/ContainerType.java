package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;

public enum ContainerType implements StringRepresentable {
    CHEST("chest", 27),
    HOPPER("hopper", 5),
    DROPPER("dropper", 9),
    DISPENSER("dispenser", 9),
    DOUBLE_CHEST("double_chest", 54);

    public static final Codec<ContainerType> CODEC = StringRepresentable.fromEnum(ContainerType::values);

    private final String name;
    private final int slots;

    ContainerType(String name, int slots) {
        this.name = name;
        this.slots = slots;
    }

    public int slots() {
        return slots;
    }

    public MenuProvider menuProvider(Component title, SimpleContainer container) {
        return new SimpleMenuProvider((id, inv, p) -> switch (this) {
            case HOPPER -> new HopperMenu(id, inv, container);
            case DROPPER, DISPENSER -> new DispenserMenu(id, inv, container);
            case CHEST -> ChestMenu.threeRows(id, inv, container);
            case DOUBLE_CHEST -> ChestMenu.sixRows(id, inv, container);
        }, title);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
