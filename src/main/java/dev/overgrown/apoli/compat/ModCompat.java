package dev.overgrown.apoli.compat;

import net.neoforged.fml.ModList;

public final class ModCompat {
    private ModCompat() {}

    public static final boolean VOICECHAT = ModList.get().isLoaded("voicechat");

    public static final boolean NERB = ModList.get().isLoaded("nerb");

    public static final boolean LAMBDYNLIGHTS = ModList.get().isLoaded("lambdynlights");

    public static final boolean ICARUS = ModList.get().isLoaded("icarus");

    public static final boolean FIGURA = ModList.get().isLoaded("figura");

    public static final boolean TRINKETS = ModList.get().isLoaded("trinkets");

    public static final boolean ACCESSORIES = ModList.get().isLoaded("accessories");

    public static final boolean CURIOS = ModList.get().isLoaded("curios");

    public static boolean anyAccessory() {
        return TRINKETS || ACCESSORIES || CURIOS;
    }

    public static final boolean HARDCORE_REVIVAL = ModList.get().isLoaded("hardcorerevival");
}
