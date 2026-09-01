package dev.overgrown.apoli.power;

import dev.overgrown.apoli.data.Expression;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public final class PowerResources {

    public interface ClientCooldownLookup {
        int remaining(Entity owner, ResourceLocation powerId);
    }

    private static ClientCooldownLookup clientCooldowns = (owner, powerId) -> 0;

    private PowerResources() {}

    public static void setClientCooldownLookup(ClientCooldownLookup lookup) {
        clientCooldowns = lookup;
    }

    public static int clientCooldown(PowerContainer holder, ResourceLocation powerId) {
        return clientCooldowns.remaining(holder.rawOwner(), powerId);
    }

    public static int cooldownTicks(Expression cooldown, @Nullable PowerContainer holder) {
        if (cooldown == null) return 0;
        return cooldown.evalIntWith(holder == null ? null : holder.rawOwner(), holder, 0.0);
    }

    public static OptionalInt read(@Nullable PowerContainer holder, ResourceLocation powerId) {
        if (holder == null || !holder.hasPower(powerId)) return OptionalInt.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded != null) {
            PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
            if (type != null) {
                OptionalInt typed = invokeRead(type, powerId, loaded.config(), holder);
                if (typed.isPresent()) return typed;
            }
        }
        return holder.getAuxInt(powerId);
    }

    public static OptionalInt write(@Nullable PowerContainer holder, ResourceLocation powerId, int value) {
        if (holder == null || !holder.hasPower(powerId)) return OptionalInt.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalInt.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return OptionalInt.empty();
        return invokeWrite(type, powerId, loaded.config(), holder, value);
    }

    public static OptionalInt readAt(@Nullable PowerContainer holder, ResourceLocation powerId, int slot) {
        if (holder == null || !holder.hasPower(powerId)) return OptionalInt.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return slot == 0 ? holder.getAuxInt(powerId) : OptionalInt.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return slot == 0 ? holder.getAuxInt(powerId) : OptionalInt.empty();
        OptionalInt typed = invokeReadAt(type, powerId, loaded.config(), holder, slot);
        if (typed.isPresent()) return typed;
        return slot == 0 ? holder.getAuxInt(powerId) : OptionalInt.empty();
    }

    public static OptionalInt writeAt(@Nullable PowerContainer holder, ResourceLocation powerId, int slot, int value) {
        if (holder == null || !holder.hasPower(powerId)) return OptionalInt.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalInt.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return OptionalInt.empty();
        return invokeWriteAt(type, powerId, loaded.config(), holder, slot, value);
    }

    public static int size(@Nullable PowerContainer holder, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return 0;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return 0;
        return invokeSize(type, powerId, loaded.config(), holder);
    }

    public static int indexOf(@Nullable PowerContainer holder, ResourceLocation powerId, int value) {
        if (holder == null || !holder.hasPower(powerId)) return -1;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return holder.getAuxIntOr(powerId, value + 1) == value ? 0 : -1;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return -1;
        return invokeIndexOf(type, powerId, loaded.config(), holder, value);
    }

    public static OptionalInt readDeadline(PowerContainer holder, ResourceLocation powerId) {
        OptionalInt stored = holder.getAuxInt(powerId);
        if (stored.isEmpty()) return OptionalInt.of(0);
        int now = (int) holder.rawOwner().level().getGameTime();
        return OptionalInt.of(Math.max(0, stored.getAsInt() - now));
    }

    public static OptionalInt writeDeadline(PowerContainer holder, ResourceLocation powerId, int remaining, int cooldown) {
        if (!(holder instanceof PowerContainerImpl impl)) return OptionalInt.empty();
        int clamped = Math.max(0, Math.min(remaining, Math.max(cooldown, 0)));
        int now = (int) holder.rawOwner().level().getGameTime();
        impl.setAuxInt(powerId, now + clamped);
        return OptionalInt.of(clamped);
    }

    public static OptionalInt bound(@Nullable PowerContainer holder, ResourceLocation powerId, boolean max) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalInt.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return OptionalInt.empty();
        if (type instanceof dev.overgrown.apoli.power.builtin.ResourcePower) {
            return dev.overgrown.apoli.power.builtin.ResourcePower.boundOf(holder, powerId, max);
        }
        return invokeBound(type, powerId, loaded.config(), holder, max);
    }

    public static boolean isResource(ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return false;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return false;
        if (type instanceof dev.overgrown.apoli.power.builtin.ResourcePower) return true;
        return invokeBound(type, powerId, loaded.config(), null, true).isPresent();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionalInt invokeRead(PowerType type, ResourceLocation powerId, Object cfg, PowerContainer holder) {
        return type.readResource(powerId, cfg, holder);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionalInt invokeWrite(PowerType type, ResourceLocation powerId, Object cfg,
                                           PowerContainer holder, int value) {
        return type.writeResource(powerId, cfg, holder, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionalInt invokeReadAt(PowerType type, ResourceLocation powerId, Object cfg,
                                            PowerContainer holder, int slot) {
        return type.readResourceAt(powerId, cfg, holder, slot);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionalInt invokeWriteAt(PowerType type, ResourceLocation powerId, Object cfg,
                                             PowerContainer holder, int slot, int value) {
        return type.writeResourceAt(powerId, cfg, holder, slot, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int invokeIndexOf(PowerType type, ResourceLocation powerId, Object cfg,
                                     PowerContainer holder, int value) {
        return type.resourceIndexOf(powerId, cfg, holder, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int invokeSize(PowerType type, ResourceLocation powerId, Object cfg,
                                  @Nullable PowerContainer holder) {
        return type.resourceSize(powerId, cfg, holder);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionalInt invokeBound(PowerType type, ResourceLocation powerId, Object cfg,
                                           @Nullable PowerContainer holder, boolean max) {
        return type.resourceBound(powerId, cfg, holder, max);
    }
}
