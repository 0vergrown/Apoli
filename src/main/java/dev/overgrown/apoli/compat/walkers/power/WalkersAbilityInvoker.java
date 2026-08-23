package dev.overgrown.apoli.compat.walkers.power;

import dev.overgrown.apoli.compat.walkers.WalkersBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class WalkersAbilityInvoker {
    private WalkersAbilityInvoker() {}

    private static final MethodHandle USE_ABILITY;

    static {
        MethodHandle handle = null;
        if (WalkersBridge.present()) {
            try {
                handle = MethodHandles.publicLookup().findStatic(
                    Class.forName("tocraft.walkers.api.PlayerAbilities"),
                    "useAbility",
                    MethodType.methodType(void.class, Player.class));
            } catch (Throwable ignored) {
            }
        }
        USE_ABILITY = handle;
    }

    public static void invoke(Entity entity) {
        if (USE_ABILITY == null || !(entity instanceof Player player)) return;
        try {
            USE_ABILITY.invoke(player);
        } catch (Throwable ignored) {
        }
    }
}
