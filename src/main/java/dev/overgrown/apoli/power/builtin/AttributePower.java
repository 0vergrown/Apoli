package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.attribute.ApoliAttributes;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AttributePower extends PowerType<AttributePower.Cfg> {

    public record Cfg(
        List<AttributeModifier> modifiers,
        boolean updateHealth
    ) {}

    private record Slot(
        Attribute attribute,
        UUID id,
        String name,
        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation,
        boolean maxHealth,
        AttributeModifier modifier
    ) {}

    private record Plan(Cfg cfg, Optional<EntityCondition> condition, Slot[] slots) {}

    private static final Map<ResourceLocation, Plan> PLANS = new ConcurrentHashMap<>();

    private static final MapCodec<Cfg> CFG_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(c -> Optional.empty()),
        AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers", List.of()).forGetter(Cfg::modifiers),
        Codec.BOOL.optionalFieldOf("update_health", true).forGetter(Cfg::updateHealth)
    ).apply(i, (single, list, updateHealth) -> {
        List<AttributeModifier> all = new ArrayList<>(list);
        single.ifPresent(all::add);
        return new Cfg(List.copyOf(all), updateHealth);
    }));

    @Override
    public MapCodec<Cfg> configCodec() {
        return CFG_CODEC;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        LivingEntity entity = holder.owner();
        if (entity == null || holder.isSuppressed(powerId)) return;
        apply(entity, holder, plan(powerId, cfg), cfg.updateHealth);
    }

    @Override
    public void onSuppressed(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        LivingEntity entity = holder.owner();
        if (entity == null) return;
        strip(entity, plan(powerId, cfg).slots, cfg.updateHealth);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        LivingEntity entity = holder.owner();
        if (entity == null) return;
        strip(entity, plan(powerId, cfg).slots, cfg.updateHealth);
    }

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        LivingEntity entity = holder.owner();
        if (entity == null) return;
        apply(entity, holder, plan(powerId, cfg), cfg.updateHealth);
    }

    public static void purge(LivingEntity entity, ResourceLocation powerId) {
        Plan plan = PLANS.get(powerId);
        if (plan != null) strip(entity, plan.slots, false);
    }

    private static Plan plan(ResourceLocation powerId, Cfg cfg) {
        Plan plan = PLANS.get(powerId);
        if (plan != null && plan.cfg == cfg) return plan;
        String seed = "apoli:attribute:" + powerId + ":";
        String fallbackName = "apoli:" + powerId;
        List<Slot> slots = new ArrayList<>(cfg.modifiers.size());
        for (int idx = 0; idx < cfg.modifiers.size(); idx++) {
            AttributeModifier modifier = cfg.modifiers.get(idx);
            if (modifier.attribute().isEmpty()) continue;
            Attribute attribute = ApoliAttributes.get(modifier.attribute().get());
            if (attribute == null) continue;
            slots.add(new Slot(attribute, UUID.nameUUIDFromBytes((seed + idx).getBytes(StandardCharsets.UTF_8)),
                modifier.name().orElse(fallbackName), modifier.operation().vanillaOperation(),
                attribute == Attributes.MAX_HEALTH, modifier));
        }
        Power loaded = ApoliPowers.get(powerId);
        plan = new Plan(cfg, loaded == null ? Optional.empty() : loaded.condition(), slots.toArray(new Slot[0]));
        PLANS.put(powerId, plan);
        return plan;
    }

    private static void apply(LivingEntity entity, PowerContainer holder, Plan plan, boolean updateHealth) {
        Slot[] slots = plan.slots;
        if (slots.length == 0) return;
        boolean active = conditionHolds(entity, plan.condition);
        for (Slot slot : slots) {
            AttributeInstance instance = entity.getAttribute(slot.attribute);
            if (instance == null) continue;
            net.minecraft.world.entity.ai.attributes.AttributeModifier current = instance.getModifier(slot.id);
            if (!active) {
                if (current != null) replace(entity, instance, slot, null, updateHealth);
                continue;
            }
            double value = slot.modifier.resolveInput(entity, holder, instance.getBaseValue());
            if (current != null && current.getOperation() == slot.operation
                    && Math.abs(current.getAmount() - value) < 1.0e-6) {
                continue;
            }
            replace(entity, instance, slot,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(slot.id, slot.name, value, slot.operation),
                updateHealth);
        }
    }

    private static void strip(LivingEntity entity, Slot[] slots, boolean updateHealth) {
        for (Slot slot : slots) {
            AttributeInstance instance = entity.getAttribute(slot.attribute);
            if (instance != null && instance.getModifier(slot.id) != null) {
                replace(entity, instance, slot, null, updateHealth);
            }
        }
    }

    private static void replace(LivingEntity entity, AttributeInstance instance, Slot slot,
                                @Nullable net.minecraft.world.entity.ai.attributes.AttributeModifier next,
                                boolean updateHealth) {
        float prevMax = slot.maxHealth ? entity.getMaxHealth() : 0f;
        float prevHealth = slot.maxHealth ? entity.getHealth() : 0f;
        instance.removeModifier(slot.id);
        if (next != null) instance.addTransientModifier(next);
        if (slot.maxHealth) reconcileHealth(entity, prevMax, prevHealth, updateHealth);
    }

    private static boolean conditionHolds(LivingEntity entity, Optional<EntityCondition> condition) {
        if (condition.isEmpty()) return true;
        if (!(entity.level() instanceof ServerLevel level)) return true;
        return condition.get().test(new EntityCtx(entity, level));
    }

    private static void reconcileHealth(LivingEntity entity, float prevMax, float prevHealth, boolean updateHealth) {
        float newMax = entity.getMaxHealth();
        if (newMax <= 0) return;
        if (updateHealth && prevMax > 0 && newMax != prevMax) {
            float ratio = prevHealth / prevMax;
            entity.setHealth(Math.max(0.1f, Math.min(newMax, newMax * ratio)));
        } else {
            entity.setHealth(Math.min(prevHealth, newMax));
        }
        syncMaxHealthAndHealth(entity);
    }

    private static void syncMaxHealthAndHealth(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || player.connection == null) return;
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), List.of(maxHealth)));
        }
        player.connection.send(new ClientboundSetHealthPacket(
            player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
    }
}
