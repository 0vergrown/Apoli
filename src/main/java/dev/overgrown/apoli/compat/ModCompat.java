package dev.overgrown.apoli.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class ModCompat {
    private ModCompat() {}

    public static final boolean ICARUS = FabricLoader.getInstance().isModLoaded("icarus");

    public static final boolean FIGURA = FabricLoader.getInstance().isModLoaded("figura");

    public static final boolean TRINKETS = FabricLoader.getInstance().isModLoaded("trinkets");

    public static final boolean ACCESSORIES = FabricLoader.getInstance().isModLoaded("accessories");

    public static final boolean CURIOS = FabricLoader.getInstance().isModLoaded("curios");

    public static boolean anyAccessory() {
        return TRINKETS || ACCESSORIES || CURIOS;
    }

    public static final boolean HARDCORE_REVIVAL = FabricLoader.getInstance().isModLoaded("hardcorerevival");

    public static final boolean VOICECHAT = FabricLoader.getInstance().isModLoaded("voicechat");
}
