package dev.overgrown.apoli.compat.figura;

import dev.overgrown.apoli.client.render.ApoliPlayerModels;
import dev.overgrown.apoli.power.builtin.ModifyPlayerModelPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FiguraModelPowerManager {
    private FiguraModelPowerManager() {}

    private record Applied(ResourceLocation modelId, Object avatarHandle) {}

    private static final Map<UUID, Applied> APPLIED = new HashMap<>();

    public static void tick(Minecraft mc) {
        if (mc.level == null) {
            APPLIED.clear();
            return;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            UUID uuid = player.getUUID();
            ResourceLocation desired = desiredModel(player);
            Applied current = APPLIED.get(uuid);

            if (desired == null) {
                if (current != null) {
                    APPLIED.remove(uuid);
                    FiguraAvatarBridge.restore(uuid);
                }
                continue;
            }

            CompoundTag nbt = FiguraAvatarCache.get(desired);
            if (nbt == null) {
                if (current != null) {
                    APPLIED.remove(uuid);
                    FiguraAvatarBridge.restore(uuid);
                }
                continue;
            }

            if (current != null
                && current.modelId().equals(desired)
                && current.avatarHandle() == FiguraAvatarBridge.mainAvatarHandle(uuid)) {
                continue;
            }

            Object handle = FiguraAvatarBridge.equip(uuid, nbt);
            APPLIED.put(uuid, new Applied(desired, handle));
        }
    }

    private static ResourceLocation desiredModel(AbstractClientPlayer player) {
        ResourceLocation modelId = ModifyPlayerModelPower.firstActiveModel(player);
        if (modelId == null || ApoliPlayerModels.isRegistered(modelId)) return null;
        return modelId;
    }

    public static void onResourcesReloaded() {
        FiguraAvatarCache.clear();
        APPLIED.clear();
    }

    public static void clear() {
        APPLIED.clear();
    }
}
