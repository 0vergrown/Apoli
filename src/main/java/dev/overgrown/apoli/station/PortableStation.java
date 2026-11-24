package dev.overgrown.apoli.station;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.recipe.PortableCraftingMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;

public enum PortableStation implements StringRepresentable {
    CRAFTING_TABLE("crafting_table", "container.crafting"),
    STONECUTTER("stonecutter", "container.stonecutter"),
    ANVIL("anvil", "container.repair"),
    SMITHING_TABLE("smithing_table", "container.upgrade"),
    LOOM("loom", "container.loom"),
    GRINDSTONE("grindstone", "container.grindstone_title"),
    CARTOGRAPHY_TABLE("cartography_table", "container.cartography_table"),
    ENCHANTING_TABLE("enchanting_table", "container.enchant");

    public static final Codec<PortableStation> CODEC = StringRepresentable.fromEnum(PortableStation::values);

    private final String name;
    private final String titleKey;

    PortableStation(String name, String titleKey) {
        this.name = name;
        this.titleKey = titleKey;
    }

    public Component defaultTitle() {
        return Component.translatable(titleKey);
    }

    public MenuProvider menuProvider(Player player, Component title) {
        ContainerLevelAccess access = ContainerLevelAccess.create(player.level(), player.blockPosition());
        return new SimpleMenuProvider((id, inv, p) -> switch (this) {
            case CRAFTING_TABLE -> new PortableCraftingMenu(id, inv, access);
            case STONECUTTER -> new PortableStonecutterMenu(id, inv, access);
            case ANVIL -> new PortableAnvilMenu(id, inv, access);
            case SMITHING_TABLE -> new PortableSmithingMenu(id, inv, access);
            case LOOM -> new PortableLoomMenu(id, inv, access);
            case GRINDSTONE -> new PortableGrindstoneMenu(id, inv, access);
            case CARTOGRAPHY_TABLE -> new PortableCartographyTableMenu(id, inv, access);
            case ENCHANTING_TABLE -> new PortableEnchantmentMenu(id, inv, access);
        }, title);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
