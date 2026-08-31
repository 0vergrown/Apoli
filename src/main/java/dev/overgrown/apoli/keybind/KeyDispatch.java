package dev.overgrown.apoli.keybind;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerKeys;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import dev.overgrown.apoli.power.builtin.InventoryPower;
import dev.overgrown.apoli.power.builtin.PreventKeyPressPower;
import dev.overgrown.apoli.power.builtin.TogglePower;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.network.payload.ForceKeyS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Set;

public final class KeyDispatch {
    private KeyDispatch() {}

    public static void force(Entity entity, String key, int duration, boolean release) {
        if (entity == null || !(entity.level() instanceof ServerLevel)) return;
        if (key == null || key.isEmpty()) return;
        if (!release && PreventKeyPressPower.blocksForcedKeys(entity, key)) return;

        if (entity instanceof ServerPlayer player) {
            ApoliNetwork.sendForceKey(player, new ForceKeyS2C(key, Math.max(1, duration), release));
        }

        if (release) {
            HeldKeys.release(entity.getUUID(), key);
            return;
        }

        HeldKeys.force(entity.getUUID(), key, Math.max(1, duration));
        if (!(entity instanceof ServerPlayer)) press(entity, key);
    }

    public static void tickForcedNonPlayer(Entity entity) {
        if (entity instanceof ServerPlayer) return;
        Set<String> forced = HeldKeys.forcedKeys(entity.getUUID());
        if (forced.isEmpty()) return;
        for (String key : forced) dispatch(entity, key, true);
    }

    public static int press(Entity entity, String key) {
        return dispatch(entity, key, false);
    }

    public static boolean blocked(Entity entity, String key) {
        if (!PreventKeyPressPower.any(entity)) return false;
        if (HeldKeys.forcedHeld(entity.getUUID(), key)) {
            return PreventKeyPressPower.blocksForcedKeys(entity, key);
        }
        return PreventKeyPressPower.blocks(entity, key);
    }

    private static int dispatch(Entity entity, String key, boolean continuousOnly) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return 0;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return 0;

        List<ResourceLocation> candidates = PowerKeys.heldPowersUsingKey(container, key);
        if (candidates.isEmpty()) return 0;
        if (blocked(entity, key)) return 0;

        EntityCtx ctx = new EntityCtx(entity, level);
        ServerPlayer player = entity instanceof ServerPlayer sp ? sp : null;
        int fired = 0;

        for (int i = 0; i < candidates.size(); i++) {
            ResourceLocation id = candidates.get(i);
            if (container.isSuppressed(id)) continue;
            Power loaded = ApoliPowers.get(id);
            if (loaded == null) continue;
            if (loaded.condition().isPresent() && !loaded.condition().get().test(ctx)) continue;

            PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
            Object cfg = loaded.config();

            if (type instanceof ActionOnKeyPressPower active && cfg instanceof ActionOnKeyPressPower.Config c) {
                if (!c.key().key().equals(key)) continue;
                if (continuousOnly && !c.key().continuous()) continue;
                if (active.tryActivate(id, c, container)) {
                    fired++;
                    if (player != null) ApoliNetwork.sendActivated(player, new PowerActivatedS2C(id, PowerResources.cooldownTicks(c.cooldown(), container)));
                }
            } else if (type instanceof FireProjectilePower fire && cfg instanceof FireProjectilePower.Config c) {
                if (c.params().key().map(Key::key).filter(key::equals).isEmpty()) continue;
                if (continuousOnly && !c.params().key().map(Key::continuous).orElse(false)) continue;
                if (fire.tryActivate(id, c, container)) {
                    fired++;
                    if (player != null) {
                        ApoliNetwork.sendActivated(player, new PowerActivatedS2C(id, PowerResources.cooldownTicks(c.params().cooldown(), container)));
                    }
                }
            } else if (type instanceof TogglePower && cfg instanceof TogglePower.Config c) {
                if (continuousOnly) continue;
                if (!c.key().key().equals(key)) continue;
                TogglePower.toggle(container, id);
                fired++;
            } else if (type instanceof InventoryPower inventory && cfg instanceof InventoryPower.Config c) {
                if (continuousOnly) continue;
                if (player == null || !(container instanceof PowerContainerImpl impl)) continue;
                if (!c.key().key().equals(key)) continue;
                inventory.open(id, c, player, impl);
                fired++;
            }
        }
        return fired;
    }
}
