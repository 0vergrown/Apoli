package dev.overgrown.apoli.power;

import dev.overgrown.apoli.data.FunctionalKey;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.keybind.ApoliKeybinds;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import dev.overgrown.apoli.power.builtin.ActionOnKeySequencePower;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import dev.overgrown.apoli.power.builtin.InventoryPower;
import dev.overgrown.apoli.power.builtin.TogglePower;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PowerKeys {

    private PowerKeys() {}

    public static void collect(Power power, Collection<String> out) {
        Object cfg = power.config();
        if (cfg instanceof ActionOnKeyPressPower.Config c) {
            out.add(c.key().key());
        } else if (cfg instanceof TogglePower.Config c) {
            out.add(c.key().key());
        } else if (cfg instanceof InventoryPower.Config c) {
            out.add(c.key().key());
        } else if (cfg instanceof FireProjectilePower.Config c) {
            c.params().key().map(Key::key).ifPresent(out::add);
        } else if (cfg instanceof ActionOnKeySequencePower.Config c) {
            for (FunctionalKey fk : c.keys()) out.add(fk.key().key());
        }
    }

    public static boolean uses(Power power, String key) {
        Set<String> keys = new LinkedHashSet<>(2);
        collect(power, keys);
        return keys.contains(key);
    }

    public static List<ResourceLocation> heldPowersUsingKey(PowerContainer container, String key) {
        List<ResourceLocation> out = new ArrayList<>();
        for (ResourceLocation id : container.allPowers()) {
            Power power = ApoliPowers.get(id);
            if (power != null && uses(power, key)) out.add(id);
        }
        return out;
    }

    public static List<String> knownKeys() {
        Set<String> out = new LinkedHashSet<>();
        out.add(Key.PRIMARY_ACTIVE);
        for (ResourceLocation id : ApoliKeybinds.view().keySet()) {
            out.add("key." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
        }
        for (Power power : ApoliPowers.view().values()) collect(power, out);
        return new ArrayList<>(out);
    }

    public static List<String> keysHeldBy(PowerContainer container) {
        Set<String> out = new LinkedHashSet<>();
        for (ResourceLocation id : container.allPowers()) {
            Power power = ApoliPowers.get(id);
            if (power != null) collect(power, out);
        }
        return new ArrayList<>(out);
    }
}
