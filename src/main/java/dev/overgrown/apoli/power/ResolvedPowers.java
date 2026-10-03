package dev.overgrown.apoli.power;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.network.payload.SyncResolvedPowersS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ResolvedPowers {
    private record Sent(ResourceLocation[] powers, int[] targets) {}

    private static final Map<UUID, Sent> SENT = new HashMap<>();
    private static final List<ResourceLocation> SCRATCH = new ArrayList<>();
    private static int[] scratchTargets = new int[8];
    private static ResourceLocation @Nullable [] types;
    private static PowerTypeUsage.Handle[] handles = new PowerTypeUsage.Handle[0];

    private ResolvedPowers() {}

    public static void tick(MinecraftServer server) {
        if (types == null) build();
        if (types.length == 0) return;
        boolean held = false;
        for (int i = 0; i < handles.length; i++) {
            if (handles[i].isHeld()) {
                held = true;
                break;
            }
        }
        if (!held && SENT.isEmpty()) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (int p = 0, n = players.size(); p < n; p++) {
            ServerPlayer player = players.get(p);
            UUID id = player.getUUID();
            Sent sent = SENT.get(id);
            if (held) {
                resolve(player, sent);
            } else {
                SCRATCH.clear();
            }
            if (matches(sent)) continue;
            int count = SCRATCH.size();
            if (count == 0) {
                SENT.remove(id);
                ApoliNetwork.sendResolvedPowers(player, new SyncResolvedPowersS2C(List.of(), new int[0]));
                continue;
            }
            ResourceLocation[] powers = SCRATCH.toArray(new ResourceLocation[0]);
            int[] targets = Arrays.copyOf(scratchTargets, count);
            SENT.put(id, new Sent(powers, targets));
            ApoliNetwork.sendResolvedPowers(player, new SyncResolvedPowersS2C(List.of(powers), targets));
        }
        SCRATCH.clear();
    }

    public static void forget(UUID player) {
        SENT.remove(player);
    }

    public static void clear() {
        SENT.clear();
        SCRATCH.clear();
        types = null;
    }

    private static void build() {
        List<ResourceLocation> found = new ArrayList<>();
        for (Map.Entry<ResourceLocation, PowerType<?>> entry : PowerTypeRegistry.view().entrySet()) {
            if (entry.getValue().resolvesForClient()) found.add(entry.getKey());
        }
        ResourceLocation[] built = found.toArray(new ResourceLocation[0]);
        PowerTypeUsage.Handle[] builtHandles = new PowerTypeUsage.Handle[built.length];
        for (int i = 0; i < built.length; i++) builtHandles[i] = PowerTypeUsage.handle(built[i]);
        handles = builtHandles;
        types = built;
    }

    private static void resolve(ServerPlayer player, @Nullable Sent sent) {
        SCRATCH.clear();
        PowerContainer container = PowerContainer.of(player);
        if (container == null || container.isEmpty()) return;
        ResourceLocation[] known = types;
        EntityCtx ctx = null;
        for (int t = 0; t < known.length; t++) {
            if (!handles[t].isHeld()) continue;
            List<ResourceLocation> powers = container.powersOfType(known[t]);
            for (int i = 0, n = powers.size(); i < n; i++) {
                ResourceLocation powerId = powers.get(i);
                if (container.isSuppressed(powerId)) continue;
                Power power = ApoliPowers.get(powerId);
                if (power == null) continue;
                PowerType<?> type = PowerTypeRegistry.get(power.typeId());
                if (type == null) continue;
                if (power.condition().isPresent()) {
                    if (ctx == null) ctx = EntityCtx.of(player, player.level());
                    if (!power.condition().get().test(ctx)) continue;
                }
                int slot = SCRATCH.size();
                if (slot == scratchTargets.length) scratchTargets = Arrays.copyOf(scratchTargets, slot * 2);
                SCRATCH.add(powerId);
                scratchTargets[slot] = invokeTarget(type, powerId, power.config(), player, previousTarget(sent, powerId));
            }
        }
    }

    private static int previousTarget(@Nullable Sent sent, ResourceLocation powerId) {
        if (sent == null) return -1;
        ResourceLocation[] powers = sent.powers();
        for (int i = 0; i < powers.length; i++) {
            if (powers[i].equals(powerId)) return sent.targets()[i];
        }
        return -1;
    }

    private static boolean matches(@Nullable Sent sent) {
        int count = SCRATCH.size();
        if (sent == null) return count == 0;
        if (sent.powers().length != count) return false;
        for (int i = 0; i < count; i++) {
            if (!sent.powers()[i].equals(SCRATCH.get(i))) return false;
            if (sent.targets()[i] != scratchTargets[i]) return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int invokeTarget(PowerType type, ResourceLocation powerId, Object cfg, ServerPlayer player, int current) {
        return type.clientTarget(powerId, cfg, player, current);
    }
}
