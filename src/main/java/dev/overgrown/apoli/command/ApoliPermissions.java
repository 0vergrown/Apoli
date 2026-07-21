package dev.overgrown.apoli.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class ApoliPermissions {

    private static final ConcurrentHashMap<String, PermissionNode<Boolean>> NODES = new ConcurrentHashMap<>();
    private static volatile boolean gathered;

    private ApoliPermissions() {}

    public static Predicate<CommandSourceStack> require(String node, int fallbackLevel) {
        PermissionNode<Boolean> permission = NODES.computeIfAbsent(node, key -> create(key, fallbackLevel));
        return source -> checkNode(source, permission, fallbackLevel);
    }

    public static boolean check(CommandSourceStack source, String node, int fallbackLevel) {
        PermissionNode<Boolean> permission = NODES.computeIfAbsent(node, key -> create(key, fallbackLevel));
        return checkNode(source, permission, fallbackLevel);
    }

    private static boolean checkNode(CommandSourceStack source, PermissionNode<Boolean> permission, int fallbackLevel) {
        if (gathered && source.getEntity() instanceof ServerPlayer player) {
            try {
                return PermissionAPI.getPermission(player, permission);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return source.hasPermission(fallbackLevel);
    }

    private static PermissionNode<Boolean> create(String node, int fallbackLevel) {
        int split = node.indexOf('.');
        String namespace = split > 0 ? node.substring(0, split) : "apoli";
        String path = split > 0 ? node.substring(split + 1) : node;
        return new PermissionNode<>(namespace, path, PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(fallbackLevel));
    }

    public static void gatherNodes(PermissionGatherEvent.Nodes event) {
        for (PermissionNode<Boolean> node : NODES.values()) {
            event.addNodes(node);
        }
        gathered = true;
    }
}
