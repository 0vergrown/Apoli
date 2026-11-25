package dev.overgrown.apoli.gui;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ApoliMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Apoli.MOD_ID);

    public static final Supplier<MenuType<GuiMenu>> GUI = MENUS.register("gui", () ->
        IMenuTypeExtension.create((id, inventory, buf) -> GuiMenu.client(id, inventory, GuiOpenData.read(buf))));

    private ApoliMenus() {}

    public static MenuType<GuiMenu> gui() {
        return GUI.get();
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }

    public static void openGui(ServerPlayer player, Component title, GuiOpenData data, MenuConstructor factory) {
        player.openMenu(new SimpleMenuProvider(factory, title), data::write);
    }
}
