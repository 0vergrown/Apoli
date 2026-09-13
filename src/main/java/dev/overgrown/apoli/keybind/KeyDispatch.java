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
import dev.overgrown.apoli.power.builtin.MultiplePower;
import dev.overgrown.apoli.power.builtin.PreventKeyPressPower;
import dev.overgrown.apoli.power.builtin.TogglePower;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.network.payload.ForceKeyS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

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

    public static boolean pressPower(Entity entity, ResourceLocation powerId, @Nullable String key) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null) return false;
        if (container.isSuppressed(powerId)) return false;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return false;
        if (key != null && blocked(entity, key)) return false;
        EntityCtx ctx = new EntityCtx(entity, level);
        if (loaded.condition().isPresent() && !loaded.condition().get().test(ctx)) return false;
        ServerPlayer player = entity instanceof ServerPlayer sp ? sp : null;
        return activate(container, player, powerId, loaded, null, false);
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
            if (activate(container, player, id, loaded, key, continuousOnly)) fired++;
        }
        return fired;
    }

    private static boolean activate(PowerContainer container, @Nullable ServerPlayer player,
                                    ResourceLocation id, Power loaded,
                                    @Nullable String key, boolean continuousOnly) {
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        Object cfg = loaded.config();

        if (type instanceof MultiplePower && cfg instanceof MultiplePower.Cfg multiple) {
            return activateSubPowers(container, player, multiple, key, continuousOnly);
        }
        if (type instanceof ActionOnKeyPressPower active && cfg instanceof ActionOnKeyPressPower.Config c) {
            if (key != null && !c.key().key().equals(key)) return false;
            if (continuousOnly && !c.key().continuous()) return false;
            if (!active.tryActivate(id, c, container)) return false;
            if (player != null) {
                ApoliNetwork.sendActivated(player,
                    new PowerActivatedS2C(id, PowerResources.cooldownTicks(c.cooldown(), container)));
            }
            return true;
        }
        if (type instanceof FireProjectilePower fire && cfg instanceof FireProjectilePower.Config c) {
            if (key != null && c.params().key().map(Key::key).filter(key::equals).isEmpty()) return false;
            if (continuousOnly && !c.params().key().map(Key::continuous).orElse(false)) return false;
            if (!fire.tryActivate(id, c, container)) return false;
            if (player != null) {
                ApoliNetwork.sendActivated(player,
                    new PowerActivatedS2C(id, PowerResources.cooldownTicks(c.params().cooldown(), container)));
            }
            return true;
        }
        if (type instanceof TogglePower && cfg instanceof TogglePower.Config c) {
            if (continuousOnly) return false;
            if (key != null && !c.key().key().equals(key)) return false;
            TogglePower.toggle(container, id);
            return true;
        }
        if (type instanceof InventoryPower inventory && cfg instanceof InventoryPower.Config c) {
            if (continuousOnly) return false;
            if (player == null || !(container instanceof PowerContainerImpl impl)) return false;
            if (key != null && !c.key().key().equals(key)) return false;
            inventory.open(id, c, player, impl);
            return true;
        }
        return false;
    }

    private static boolean activateSubPowers(PowerContainer container, @Nullable ServerPlayer player,
                                             MultiplePower.Cfg cfg, @Nullable String key, boolean continuousOnly) {
        List<ResourceLocation> subs = cfg.subPowerIds();
        if (subs.isEmpty()) return false;
        Entity owner = container.rawOwner();
        if (!(owner.level() instanceof ServerLevel level)) return false;
        EntityCtx ctx = null;
        boolean fired = false;
        for (int i = 0; i < subs.size(); i++) {
            ResourceLocation subId = subs.get(i);
            if (container.isSuppressed(subId)) continue;
            Power sub = ApoliPowers.get(subId);
            if (sub == null) continue;
            if (PowerTypeRegistry.get(sub.typeId()) instanceof MultiplePower) continue;
            if (sub.condition().isPresent()) {
                if (ctx == null) ctx = new EntityCtx(owner, level);
                if (!sub.condition().get().test(ctx)) continue;
            }
            if (activate(container, player, subId, sub, key, continuousOnly)) fired = true;
        }
        return fired;
    }
}
