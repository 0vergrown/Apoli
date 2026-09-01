package dev.overgrown.apoli.client;

import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.power.builtin.PreventKeyPressPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class BlockedKeys {

    private static boolean any;
    private static boolean all;
    private static boolean scroll;
    private static boolean forced;
    private static Set<String> named = Set.of();

    private BlockedKeys() {}

    public static void tick() {
        Player player = Minecraft.getInstance().player;
        if (!any && !PreventKeyPressPower.any(player)) return;

        boolean[] flags = new boolean[]{false, false, false, false, false};
        Set<String> keys = new HashSet<>();
        PreventKeyPressPower.forEachActive(player, cfg -> {
            flags[0] = true;
            if (cfg.blocksEverything()) flags[1] = true;
            if (cfg.blocksScroll()) flags[2] = true;
            if (cfg.affectForced()) flags[3] = true;
            if (cfg.unpress()) flags[4] = true;
            cfg.keys().ifPresent(list -> {
                for (Key key : list) keys.add(key.key());
            });
        });

        boolean wasAny = any;
        any = flags[0];
        all = flags[1];
        scroll = flags[2];
        forced = flags[3];
        named = keys.isEmpty() ? Set.of() : Set.copyOf(keys);

        if (any != wasAny && (!any || flags[4])) KeyMapping.releaseAll();
    }

    public static void clear() {
        boolean wasAny = any;
        any = false;
        all = false;
        scroll = false;
        forced = false;
        named = Set.of();
        if (wasAny) KeyMapping.releaseAll();
    }

    public static boolean active() {
        return any;
    }

    public static boolean blocksScroll() {
        return any && scroll;
    }

    public static boolean blocks(KeyMapping mapping) {
        if (!any) return false;
        if (!forced && ForcedKeys.isForced(mapping)) return false;
        return all || named.contains(mapping.getName());
    }

    public static boolean blocks(String name) {
        if (!any) return false;
        return all || named.contains(name);
    }
}
