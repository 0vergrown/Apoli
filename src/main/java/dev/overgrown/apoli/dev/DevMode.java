package dev.overgrown.apoli.dev;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DevMode {

    private static final Set<UUID> ENABLED = new HashSet<>();
    private static volatile boolean any;

    private DevMode() {}

    public static boolean any() {
        return any;
    }

    public static boolean toggle(ServerPlayer player) {
        boolean enabled = !ENABLED.remove(player.getUUID());
        if (enabled) ENABLED.add(player.getUUID());
        any = !ENABLED.isEmpty();
        dev.overgrown.apoli.ApoliNetwork.sendDevMode(player,
            new dev.overgrown.apoli.network.payload.DevModeS2C(enabled));
        return enabled;
    }

    public static boolean isEnabled(@Nullable Entity entity) {
        return any && entity != null && ENABLED.contains(entity.getUUID());
    }

    /** With dev mode on, show the actor the command exactly as it will run, macros already expanded. */
    public static void echoCommand(@Nullable Entity actor, String command) {
        if (!any || !(actor instanceof ServerPlayer player) || !ENABLED.contains(player.getUUID())) return;
        player.sendSystemMessage(net.minecraft.network.chat.Component
            .literal("/" + command)
            .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }

    public static void forget(UUID uuid) {
        if (ENABLED.remove(uuid)) any = !ENABLED.isEmpty();
    }

    public static List<ServerPlayer> watchers(@Nullable ServerLevel level) {
        if (!any || level == null) return List.of();
        MinecraftServer server = level.getServer();
        if (server == null) return List.of();
        List<ServerPlayer> out = null;
        for (UUID uuid : ENABLED) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null || player.level() != level) continue;
            if (out == null) out = new ArrayList<>(2);
            out.add(player);
        }
        return out == null ? List.of() : out;
    }
}
