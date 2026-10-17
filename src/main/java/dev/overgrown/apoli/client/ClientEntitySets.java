package dev.overgrown.apoli.client;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientEntitySets {

    private record Members(List<UUID> list, Set<UUID> set) {
        static Members of(List<UUID> members) {
            List<UUID> list = List.copyOf(members);
            return new Members(list, Set.copyOf(list));
        }
    }

    public static final EntitySetPower.ClientView VIEW = new EntitySetPower.ClientView() {
        @Override
        public boolean contains(Entity owner, ResourceLocation powerId, Entity member) {
            Members members = lookup(owner, powerId);
            return members != null && members.set().contains(member.getUUID());
        }

        @Override
        public int size(Entity owner, ResourceLocation powerId) {
            Members members = lookup(owner, powerId);
            return members == null ? 0 : members.list().size();
        }
    };

    private static volatile Map<ResourceLocation, Members> own = Map.of();
    private static volatile Map<UUID, Map<ResourceLocation, Members>> tracked = Map.of();

    private ClientEntitySets() {}

    public static synchronized void apply(ResourceLocation powerId, List<UUID> members) {
        own = with(own, powerId, members);
    }

    public static synchronized void applyTracked(UUID owner, ResourceLocation powerId, List<UUID> members) {
        Map<ResourceLocation, Members> sets = with(tracked.getOrDefault(owner, Map.of()), powerId, members);
        Map<UUID, Map<ResourceLocation, Members>> next = new HashMap<>(tracked);
        if (sets.isEmpty()) next.remove(owner);
        else next.put(owner, sets);
        tracked = Map.copyOf(next);
    }

    private static Map<ResourceLocation, Members> with(Map<ResourceLocation, Members> sets,
                                                       ResourceLocation powerId, List<UUID> members) {
        Map<ResourceLocation, Members> next = new HashMap<>(sets);
        if (members.isEmpty()) next.remove(powerId);
        else next.put(powerId, Members.of(members));
        return Map.copyOf(next);
    }

    public static List<UUID> members(ResourceLocation powerId) {
        Members members = own.get(powerId);
        return members == null ? List.of() : members.list();
    }

    private static @Nullable Members lookup(Entity owner, ResourceLocation powerId) {
        if (owner == Minecraft.getInstance().player) return own.get(powerId);
        Map<ResourceLocation, Members> sets = tracked.get(owner.getUUID());
        return sets == null ? null : sets.get(powerId);
    }

    public static synchronized void clear() {
        own = Map.of();
        tracked = Map.of();
        WARNED.clear();
    }

    private static final Set<ResourceLocation> WARNED = new HashSet<>();

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
