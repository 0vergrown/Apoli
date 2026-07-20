package dev.overgrown.apoli.command;

import net.minecraft.commands.CommandSourceStack;

import java.util.function.Predicate;

public final class ApoliPermissions {

    private static final boolean PERMISSIONS_API_LOADED = ApoliPermissions.class.getClassLoader()
        .getResource("me/lucko/fabric/api/permissions/v0/Permissions.class") != null;

    private ApoliPermissions() {}

    public static Predicate<CommandSourceStack> require(String node, int fallbackLevel) {
        if (PERMISSIONS_API_LOADED) {
            return source -> FabricPermissionsApi.check(source, node, fallbackLevel);
        }
        return source -> source.hasPermission(fallbackLevel);
    }

    public static boolean check(CommandSourceStack source, String node, int fallbackLevel) {
        if (PERMISSIONS_API_LOADED) {
            return FabricPermissionsApi.check(source, node, fallbackLevel);
        }
        return source.hasPermission(fallbackLevel);
    }

    private static final class FabricPermissionsApi {
        private FabricPermissionsApi() {}

        static boolean check(CommandSourceStack source, String node, int fallbackLevel) {
            return me.lucko.fabric.api.permissions.v0.Permissions.check(source, node, fallbackLevel);
        }
    }
}
