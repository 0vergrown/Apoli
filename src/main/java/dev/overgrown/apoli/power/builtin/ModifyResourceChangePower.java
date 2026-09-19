package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class ModifyResourceChangePower extends PowerType<ModifyResourceChangePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_resource_change");

    public enum Direction implements StringRepresentable {
        GAIN("gain"),
        DRAIN("drain"),
        BOTH("both");

        public static final Codec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);

        private final String name;

        Direction(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        boolean covers(boolean gaining) {
            return this == BOTH || (gaining ? this == GAIN : this == DRAIN);
        }
    }

    public record Config(Optional<AttributeModifier> modifier,
                         Optional<List<AttributeModifier>> modifiers,
                         Direction modify,
                         boolean resources,
                         boolean cooldowns,
                         Optional<ResourceLocation> resource) {
        public List<AttributeModifier> flattened() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            Direction.CODEC.optionalFieldOf("modify", Direction.BOTH).forGetter(Config::modify),
            Codec.BOOL.optionalFieldOf("resources", true).forGetter(Config::resources),
            Codec.BOOL.optionalFieldOf("cooldowns", true).forGetter(Config::cooldowns),
            IdCodecs.ID.optionalFieldOf("resource").forGetter(Config::resource)
        ).apply(i, Config::new));
    }

    private static volatile int cachedGeneration = -1;
    private static volatile boolean cachedInUse;
    private static volatile ResourceLocation[] cooldownTypes = new ResourceLocation[0];

    public static boolean inUse() {
        int generation = ApoliPowers.generation();
        if (generation != cachedGeneration) {
            cachedInUse = ApoliPowers.anyOfType(CANONICAL);
            cachedGeneration = generation;
        }
        return cachedInUse;
    }

    private static ResourceLocation[] cooldownTypes() {
        ResourceLocation[] known = cooldownTypes;
        if (known.length > 0) return known;
        List<ResourceLocation> found = new ArrayList<>();
        for (Map.Entry<ResourceLocation, PowerType<?>> entry : PowerTypeRegistry.view().entrySet()) {
            if (entry.getValue().isCooldown()) found.add(entry.getKey());
        }
        ResourceLocation[] built = found.toArray(new ResourceLocation[0]);
        cooldownTypes = built;
        return built;
    }

    private static boolean applying;

    public static int adjust(@Nullable PowerContainer container, ResourceLocation target,
                             int current, int proposed) {
        if (applying || current == proposed) return proposed;
        if (container == null || !inUse()) return proposed;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        if (powers.isEmpty()) return proposed;
        Entity owner = container.rawOwner();
        if (owner == null || owner.level().isClientSide()) return proposed;

        boolean cooldown = isCooldownPower(target);
        boolean gaining = proposed > current;
        double magnitude = Math.abs((double) proposed - current);
        double scaled = magnitude;
        EntityCtx ctx = null;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (cooldown ? !cfg.cooldowns() : !cfg.resources()) continue;
            if (!cfg.modify().covers(gaining)) continue;
            if (cfg.resource().isPresent() && !cfg.resource().get().equals(target)) continue;
            List<AttributeModifier> mods = cfg.flattened();
            if (mods.isEmpty()) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(owner, owner.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            scaled = AttributeModifierHelper.apply(scaled, mods, owner, container);
        }
        if (scaled == magnitude) return proposed;
        long shifted = Math.round(gaining ? current + scaled : current - scaled);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, shifted));
    }

    private static boolean isCooldownPower(ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return false;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        return type != null && type.isCooldown();
    }

    private record CarryKey(UUID entity, ResourceLocation power) {}

    private final Map<CarryKey, Float> carry = new HashMap<>();

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (!cfg.cooldowns()) return;
        if (!powerId.equals(cooldownDriver(holder))) return;
        Entity owner = holder.rawOwner();
        if (owner == null || owner.level().isClientSide()) return;

        ResourceLocation[] types = cooldownTypes();
        for (int t = 0; t < types.length; t++) {
            List<ResourceLocation> cooldowns = holder.powersOfType(types[t]);
            for (int i = 0, n = cooldowns.size(); i < n; i++) {
                accelerate(holder, owner.getUUID(), cooldowns.get(i));
            }
        }
    }

    private static @Nullable ResourceLocation cooldownDriver(PowerContainer holder) {
        List<ResourceLocation> powers = holder.powersOfType(CANONICAL);
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation candidate = powers.get(i);
            if (holder.isSuppressed(candidate)) continue;
            Power power = ApoliPowers.get(candidate);
            if (power != null && power.config() instanceof Config cfg && cfg.cooldowns()) return candidate;
        }
        return null;
    }

    private void accelerate(PowerContainer holder, UUID entity, ResourceLocation cooldownId) {
        if (holder.isSuppressed(cooldownId)) return;
        OptionalInt remaining = PowerResources.read(holder, cooldownId);
        if (remaining.isEmpty()) return;
        int value = remaining.getAsInt();
        CarryKey key = new CarryKey(entity, cooldownId);
        if (value <= 0) {
            carry.remove(key);
            return;
        }
        int stepped = adjust(holder, cooldownId, value, value - 1);
        float extra = (value - 1) - stepped + carry.getOrDefault(key, 0.0F);
        int whole = (int) extra;
        float fraction = extra - whole;
        if (fraction < 0.0F) {
            whole--;
            fraction += 1.0F;
        }
        carry.put(key, fraction);
        if (whole == 0) return;
        applying = true;
        try {
            PowerResources.write(holder, cooldownId, Math.max(0, value - whole));
        } finally {
            applying = false;
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.allPowers().contains(powerId)) return;
        UUID owner = holder.rawOwner() == null ? null : holder.rawOwner().getUUID();
        if (owner == null) return;
        carry.keySet().removeIf(key -> key.entity().equals(owner));
    }
}
