package dev.overgrown.apoli.dev;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DevMode {

    private static final Set<UUID> ENABLED = new HashSet<>();
    private static final Map<Topic, Long> LAST_REPORT = new HashMap<>();
    private static final int MAX_TOPICS = 4096;
    private static volatile boolean any;

    private record Topic(UUID subject, Object topic) {}

    private DevMode() {}

    public static boolean any() {
        return any;
    }

    public static boolean toggle(ServerPlayer player) {
        boolean enabled = !ENABLED.remove(player.getUUID());
        if (enabled) ENABLED.add(player.getUUID());
        any = !ENABLED.isEmpty();
        if (!any) clearThrottles();
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

    public static void report(@Nullable Entity subject, String message) {
        if (!any || subject == null) return;
        if (!(subject.level() instanceof ServerLevel level)) return;
        List<ServerPlayer> watchers = watchers(level);
        if (watchers.isEmpty()) return;
        net.minecraft.network.chat.Component text = net.minecraft.network.chat.Component
            .literal("[apoli] " + subject.getName().getString() + ": " + message)
            .withStyle(net.minecraft.ChatFormatting.RED);
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).sendSystemMessage(text);
        }
    }

    public static void reportEvery(@Nullable Entity subject, Object topic, int ticks, String message) {
        if (!any || subject == null) return;
        long now = subject.level().getGameTime();
        Topic key = new Topic(subject.getUUID(), topic);
        Long last = LAST_REPORT.get(key);
        if (last != null && now >= last && now - last < ticks) return;
        if (LAST_REPORT.size() >= MAX_TOPICS) LAST_REPORT.clear();
        LAST_REPORT.put(key, now);
        report(subject, message);
    }

    public static void announce(MinecraftServer server, List<Component> lines) {
        if (!any || lines.isEmpty()) return;
        for (UUID uuid : ENABLED) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) continue;
            for (int i = 0; i < lines.size(); i++) {
                player.sendSystemMessage(lines.get(i));
            }
        }
    }

    public static void forget(UUID uuid) {
        if (ENABLED.remove(uuid)) any = !ENABLED.isEmpty();
        if (!any) clearThrottles();
    }

    private static void clearThrottles() {
        LAST_REPORT.clear();
        DevParticles.forget();
    }

    /** Every dev-mode player in this level, so debug particles reach the people who asked for them. */
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
