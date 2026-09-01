package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public final class ClientRenderFlags {

    public static final int PREVENT_ENTITY_RENDER = 1;
    public static final int PREVENT_ENTITY_SELECTION = 1 << 1;
    public static final int ENTITY_GLOW = 1 << 2;
    public static final int PREVENT_BLOCK_SELECTION = 1 << 3;
    public static final int PREVENT_FEATURE_RENDER = 1 << 4;

    private static volatile int flags;

    private ClientRenderFlags() {}

    public static boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public static void clientTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            flags = 0;
            return;
        }
        PowerContainer container = PowerContainer.of(player);
        if (container == null || container.isEmpty()) {
            flags = 0;
            return;
        }
        int next = bit(container, ApoliIds.PREVENT_ENTITY_RENDER, PREVENT_ENTITY_RENDER)
            | bit(container, ApoliIds.PREVENT_ENTITY_SELECTION, PREVENT_ENTITY_SELECTION)
            | bit(container, ApoliIds.ENTITY_GLOW, ENTITY_GLOW)
            | bit(container, ApoliIds.PREVENT_BLOCK_SELECTION, PREVENT_BLOCK_SELECTION)
            | bit(container, ApoliIds.PREVENT_FEATURE_RENDER, PREVENT_FEATURE_RENDER);
        flags = next;
    }

    public static void clear() {
        flags = 0;
    }

    private static int bit(PowerContainer container, ResourceLocation typeId, int flag) {
        return container.powersOfType(typeId).isEmpty() ? 0 : flag;
    }
}
