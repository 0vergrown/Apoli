package dev.overgrown.apoli.client;

import dev.overgrown.apoli.network.payload.SyncResolvedPowersS2C;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientResolvedPowers {
    private static Map<ResourceLocation, Integer> targets = Map.of();
    private static final Map<ResourceLocation, Long> SINCE = new HashMap<>();
    private static long ticks;

    private ClientResolvedPowers() {}

    public static void apply(SyncResolvedPowersS2C payload) {
        List<ResourceLocation> powers = payload.powers();
        int[] payloadTargets = payload.targets();
        Map<ResourceLocation, Integer> next = new HashMap<>(Math.max(4, powers.size() * 2));
        for (int i = 0; i < powers.size(); i++) next.put(powers.get(i), payloadTargets[i]);
        SINCE.keySet().retainAll(next.keySet());
        for (ResourceLocation id : next.keySet()) SINCE.putIfAbsent(id, ticks);
        targets = next;
    }

    public static boolean isActive(ResourceLocation powerId) {
        return targets.containsKey(powerId);
    }

    public static boolean isEmpty() {
        return targets.isEmpty();
    }

    public static int target(ResourceLocation powerId) {
        Integer target = targets.get(powerId);
        return target == null ? -1 : target;
    }

    public static float age(ResourceLocation powerId, float partialTick) {
        Long since = SINCE.get(powerId);
        if (since == null) return 0.0F;
        return (ticks - since) + partialTick;
    }

    public static void tick(Minecraft mc) {
        if (mc.player != null && !mc.isPaused()) ticks++;
    }

    public static void clear() {
        targets = Map.of();
        SINCE.clear();
    }
}
