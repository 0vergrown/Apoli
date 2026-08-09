package dev.overgrown.apoli.compat.catalogue;

import dev.overgrown.apoli.client.config.ApoliConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public final class CatalogueConfigFactory {

    private CatalogueConfigFactory() {}

    public static Screen createConfigScreen(Screen currentScreen, ModContainer container) {
        return new ApoliConfigScreen(currentScreen);
    }
}
