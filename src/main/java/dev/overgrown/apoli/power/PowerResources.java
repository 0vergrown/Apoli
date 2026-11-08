package dev.overgrown.apoli.power;

import dev.overgrown.apoli.data.Expression;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.OptionalDouble;

public final class PowerResources {

    public static final double PRECISION = 1.0e6;
    private static final double ROUNDING_LIMIT = 9.0e9;

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

    public static double canonical(double value) {
        if (!Double.isFinite(value)) return 0.0;
        if (value == Math.rint(value) || Math.abs(value) >= ROUNDING_LIMIT) return value == 0.0 ? 0.0 : value;
        double rounded = Math.rint(value * PRECISION) / PRECISION;
        return rounded == 0.0 ? 0.0 : rounded;
    }

    public static int whole(double value) {
        if (!Double.isFinite(value)) return 0;
        long rounded = Math.round(value);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rounded));
    }

    public static boolean isWhole(double value) {
        return value == Math.rint(value);
    }

    public static String format(double value) {
        double shown = canonical(value);
        if (isWhole(shown) && Math.abs(shown) < 1.0e15) return Long.toString((long) shown);
        return BigDecimal.valueOf(shown).stripTrailingZeros().toPlainString();
    }

    public static OptionalDouble read(@Nullable PowerContainer holder, ResourceLocation powerId) {
        if (holder == null || !holder.hasPower(powerId)) return OptionalDouble.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded != null) {
            PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
            if (type != null) {
                OptionalDouble typed = invokeRead(type, powerId, loaded.config(), holder);
                if (typed.isPresent()) return typed;
            }
        }
        return holder.getAuxDouble(powerId);
    }

    public static OptionalDouble write(@Nullable PowerContainer holder, ResourceLocation powerId, double value) {
        if (holder == null || !holder.hasPower(powerId)) return OptionalDouble.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalDouble.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return OptionalDouble.empty();
        double target = value;
        if (dev.overgrown.apoli.power.builtin.ModifyResourceChangePower.inUse()) {
            OptionalDouble current = invokeRead(type, powerId, loaded.config(), holder);
            double before = current.isPresent() ? current.getAsDouble() : holder.getAuxDoubleOr(powerId, value);
            target = dev.overgrown.apoli.power.builtin.ModifyResourceChangePower.adjust(holder, powerId, before, value);
        }
        OptionalDouble written = invokeWrite(type, powerId, loaded.config(), holder, target);
        if (written.isPresent()) {
            dev.overgrown.apoli.advancement.ApoliCriteria.resourceChanged(holder.rawOwner(), powerId, written.getAsDouble());
        }
        return written;
    }

    public static OptionalDouble readAt(@Nullable PowerContainer holder, ResourceLocation powerId, int slot) {
        if (holder == null || !holder.hasPower(powerId)) return OptionalDouble.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return slot == 0 ? holder.getAuxDouble(powerId) : OptionalDouble.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return slot == 0 ? holder.getAuxDouble(powerId) : OptionalDouble.empty();
        OptionalDouble typed = invokeReadAt(type, powerId, loaded.config(), holder, slot);
        if (typed.isPresent()) return typed;
        return slot == 0 ? holder.getAuxDouble(powerId) : OptionalDouble.empty();
    }

    public static OptionalDouble writeAt(@Nullable PowerContainer holder, ResourceLocation powerId, int slot, double value) {
        if (holder == null || !holder.hasPower(powerId)) return OptionalDouble.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalDouble.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return OptionalDouble.empty();
        double target = value;
        if (dev.overgrown.apoli.power.builtin.ModifyResourceChangePower.inUse()) {
            OptionalDouble current = invokeReadAt(type, powerId, loaded.config(), holder, slot);
            double before = current.isPresent() ? current.getAsDouble() : holder.getAuxDoubleOr(powerId, value);
            target = dev.overgrown.apoli.power.builtin.ModifyResourceChangePower.adjust(holder, powerId, before, value);
        }
        OptionalDouble written = invokeWriteAt(type, powerId, loaded.config(), holder, slot, target);
        if (written.isPresent()) {
            dev.overgrown.apoli.advancement.ApoliCriteria.resourceChanged(holder.rawOwner(), powerId, written.getAsDouble());
        }
        return written;
    }

    public static int size(@Nullable PowerContainer holder, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return 0;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return 0;
        return invokeSize(type, powerId, loaded.config(), holder);
    }

    public static int indexOf(@Nullable PowerContainer holder, ResourceLocation powerId, double value) {
        if (holder == null || !holder.hasPower(powerId)) return -1;
        double wanted = canonical(value);
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return holder.getAuxDoubleOr(powerId, Double.NaN) == wanted ? 0 : -1;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return -1;
        return invokeIndexOf(type, powerId, loaded.config(), holder, wanted);
    }

    public static OptionalDouble readDeadline(PowerContainer holder, ResourceLocation powerId) {
        if (dev.overgrown.apoli.dev.DevMode.isEnabled(holder.rawOwner())) return OptionalDouble.of(0);
        double stored = holder.getAuxDoubleOr(powerId, Double.NaN);
        if (Double.isNaN(stored)) return OptionalDouble.of(0);
        int now = (int) holder.rawOwner().level().getGameTime();
        return OptionalDouble.of(Math.max(0, (int) stored - now));
    }

    public static OptionalDouble writeDeadline(PowerContainer holder, ResourceLocation powerId, double remaining, int cooldown) {
        if (!(holder instanceof PowerContainerImpl impl)) return OptionalDouble.empty();
        int clamped = Math.max(0, Math.min(whole(remaining), Math.max(cooldown, 0)));
        int now = (int) holder.rawOwner().level().getGameTime();
        impl.setAuxInt(powerId, now + clamped);
        return OptionalDouble.of(clamped);
    }

    public static OptionalDouble bound(@Nullable PowerContainer holder, ResourceLocation powerId, boolean max) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalDouble.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return OptionalDouble.empty();
        if (type instanceof dev.overgrown.apoli.power.builtin.ResourcePower) {
            return dev.overgrown.apoli.power.builtin.ResourcePower.boundOf(holder, powerId, max);
        }
        return invokeBound(type, powerId, loaded.config(), holder, max);
    }

    public static boolean triggerCooldown(@Nullable PowerContainer holder, ResourceLocation powerId) {
        if (holder == null || !holder.hasPower(powerId)) return false;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return false;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null || !type.isCooldown()) return false;
        if (invokeRead(type, powerId, loaded.config(), holder).orElse(0) > 0) return false;
        OptionalDouble length = invokeBound(type, powerId, loaded.config(), holder, true);
        if (length.isEmpty() || length.getAsDouble() <= 0) return false;
        return invokeWrite(type, powerId, loaded.config(), holder, length.getAsDouble()).isPresent();
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
    private static OptionalDouble invokeRead(PowerType type, ResourceLocation powerId, Object cfg, PowerContainer holder) {
        return type.readResource(powerId, cfg, holder);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionalDouble invokeWrite(PowerType type, ResourceLocation powerId, Object cfg,
                                              PowerContainer holder, double value) {
        return type.writeResource(powerId, cfg, holder, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionalDouble invokeReadAt(PowerType type, ResourceLocation powerId, Object cfg,
                                               PowerContainer holder, int slot) {
        return type.readResourceAt(powerId, cfg, holder, slot);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionalDouble invokeWriteAt(PowerType type, ResourceLocation powerId, Object cfg,
                                                PowerContainer holder, int slot, double value) {
        return type.writeResourceAt(powerId, cfg, holder, slot, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int invokeIndexOf(PowerType type, ResourceLocation powerId, Object cfg,
                                     PowerContainer holder, double value) {
        return type.resourceIndexOf(powerId, cfg, holder, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int invokeSize(PowerType type, ResourceLocation powerId, Object cfg,
                                  @Nullable PowerContainer holder) {
        return type.resourceSize(powerId, cfg, holder);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionalDouble invokeBound(PowerType type, ResourceLocation powerId, Object cfg,
                                              @Nullable PowerContainer holder, boolean max) {
        return type.resourceBound(powerId, cfg, holder, max);
    }
}
