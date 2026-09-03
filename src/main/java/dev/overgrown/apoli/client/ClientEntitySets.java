package dev.overgrown.apoli.client;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientEntitySets {

    private static final Map<ResourceLocation, List<UUID>> SETS = new HashMap<>();

    private ClientEntitySets() {}

    public static synchronized void apply(ResourceLocation powerId, List<UUID> members) {
        if (members.isEmpty()) SETS.remove(powerId);
        else SETS.put(powerId, List.copyOf(members));
    }

    public static synchronized List<UUID> members(ResourceLocation powerId) {
        List<UUID> members = SETS.get(powerId);
        return members == null ? List.of() : members;
    }

    public static synchronized void clear() {
        SETS.clear();
        WARNED.clear();
    }

    private static final Set<ResourceLocation> WARNED = new HashSet<>();

    /** One line in the log when an overlay names a `set` that is not an apoli:entity_set the client knows about. */
    public static synchronized void warnIfUnknown(ResourceLocation setId) {
        if (!WARNED.add(setId)) return;
        Power power = ApoliPowers.get(setId);
        if (power == null) {
            Apoli.LOGGER.warn("[Apoli] An apoli:overlay names \"set\": \"{}\", but no power with that id is loaded. "
                + "The id must be the apoli:entity_set power itself — remember '*:*' expands to the id of the file it "
                + "is written in, so '*:*_set' in powers/hud.json means '<namespace>:hud_set'.", setId);
            return;
        }
        if (!(PowerTypeRegistry.get(power.typeId()) instanceof EntitySetPower)) {
            Apoli.LOGGER.warn("[Apoli] An apoli:overlay names \"set\": \"{}\", but that power is {}, not "
                + "apoli:entity_set.", setId, power.typeId());
            return;
        }
        Apoli.LOGGER.info("[Apoli] apoli:overlay set {} is currently empty on this client. If the set has members "
            + "server-side, check that the set is owned by *you* — overlays only read the viewing player's own sets.",
            setId);
    }
}
