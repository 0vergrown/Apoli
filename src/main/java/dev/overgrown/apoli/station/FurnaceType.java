package dev.overgrown.apoli.station;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

public enum FurnaceType implements StringRepresentable {
    FURNACE("furnace", RecipeType.SMELTING, "container.furnace", false),
    BLAST_FURNACE("blast_furnace", RecipeType.BLASTING, "container.blast_furnace", true),
    SMOKER("smoker", RecipeType.SMOKING, "container.smoker", true);

    public static final Codec<FurnaceType> CODEC = StringRepresentable.fromEnum(FurnaceType::values);

    private final String name;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    private final String titleKey;
    private final boolean halvedFuel;

    FurnaceType(String name, RecipeType<? extends AbstractCookingRecipe> recipeType, String titleKey, boolean halvedFuel) {
        this.name = name;
        this.recipeType = recipeType;
        this.titleKey = titleKey;
        this.halvedFuel = halvedFuel;
    }

    public RecipeType<? extends AbstractCookingRecipe> recipeType() {
        return recipeType;
    }

    public boolean halvedFuel() {
        return halvedFuel;
    }

    public Component defaultTitle() {
        return Component.translatable(titleKey);
    }

    public AbstractFurnaceMenu createMenu(int id, Inventory inventory, Container container, ContainerData data) {
        container.startOpen(inventory.player);
        return switch (this) {
            case FURNACE -> new FurnaceMenu(id, inventory, container, data) {
                @Override
                public void removed(Player player) {
                    super.removed(player);
                    container.stopOpen(player);
                }
            };
            case BLAST_FURNACE -> new BlastFurnaceMenu(id, inventory, container, data) {
                @Override
                public void removed(Player player) {
                    super.removed(player);
                    container.stopOpen(player);
                }
            };
            case SMOKER -> new SmokerMenu(id, inventory, container, data) {
                @Override
                public void removed(Player player) {
                    super.removed(player);
                    container.stopOpen(player);
                }
            };
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
