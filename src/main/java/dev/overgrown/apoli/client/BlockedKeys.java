package dev.overgrown.apoli.client;

import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.mixin.keybinding.KeyMappingClickCountAccessor;
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
        boolean wasAll = all;
        Set<String> wasNamed = named;
        any = flags[0];
        all = flags[1];
        scroll = flags[2];
        forced = flags[3];
        if (!keys.equals(named)) named = keys.isEmpty() ? Set.of() : Set.copyOf(keys);

        if (any != wasAny || all != wasAll || named != wasNamed) settle(wasAny, wasAll, wasNamed, flags[4]);
    }

    public static void clear() {
        boolean wasAny = any;
        boolean wasAll = all;
        Set<String> wasNamed = named;
        any = false;
        all = false;
        scroll = false;
        forced = false;
        named = Set.of();
        if (wasAny) settle(true, wasAll, wasNamed, false);
    }

    private static void settle(boolean wasAny, boolean wasAll, Set<String> wasNamed, boolean unpress) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        for (KeyMapping mapping : mc.options.keyMappings) {
            String name = mapping.getName();
            boolean before = wasAny && (wasAll || wasNamed.contains(name));
            boolean now = any && (all || named.contains(name));
            if (before == now || now && !unpress) continue;
            if (now) mapping.setDown(false);
            ((KeyMappingClickCountAccessor) mapping).apoli$setClickCount(0);
        }
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
