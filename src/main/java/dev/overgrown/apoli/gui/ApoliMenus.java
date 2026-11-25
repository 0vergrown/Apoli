package dev.overgrown.apoli.gui;

import dev.overgrown.apoli.Apoli;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;

public final class ApoliMenus {
    public static final ExtendedScreenHandlerType<GuiMenu, GuiOpenData> GUI =
        new ExtendedScreenHandlerType<>(GuiMenu::client, GuiOpenData.STREAM_CODEC);

    private ApoliMenus() {}

    public static MenuType<GuiMenu> gui() {
        return GUI;
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MENU, Apoli.id("gui"), GUI);
    }

    public static void openGui(ServerPlayer player, Component title, GuiOpenData data, MenuConstructor factory) {
        player.openMenu(new ExtendedScreenHandlerFactory<GuiOpenData>() {
            @Override
            public GuiOpenData getScreenOpeningData(ServerPlayer opener) {
                return data;
            }

            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
                return factory.createMenu(id, inventory, opener);
            }
        });
    }
}
