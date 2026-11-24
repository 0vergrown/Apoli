package dev.overgrown.apoli.station;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;

public final class PortableGrindstoneMenu extends GrindstoneMenu {
    public PortableGrindstoneMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(containerId, inventory, access);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
