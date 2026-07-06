package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.ExpressionContext;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AttributePower extends PowerType<AttributePower.Cfg> {

    private static final String MAX_HEALTH_ID = "minecraft:generic.max_health";

    public record Cfg(
        List<AttributeModifier> modifiers,
        boolean updateHealth
    ) {}

    private static final java.util.Map<TrackKey, Double> APPLIED = new java.util.concurrent.ConcurrentHashMap<>();

    private record TrackKey(java.util.UUID entityUUID, ResourceLocation powerId, int idx) {}

    private static final MapCodec<Cfg> CFG_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(c -> Optional.empty()),
        AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers", List.of()).forGetter(Cfg::modifiers),
        Codec.BOOL.optionalFieldOf("update_health", true).forGetter(Cfg::updateHealth)
    ).apply(i, (single, list, updateHealth) -> {
        List<AttributeModifier> all = new java.util.ArrayList<>(list);
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
        for (int idx = 0; idx < cfg.modifiers.size(); idx++) {
            reapplyOne(entity, holder, powerId, idx, cfg.modifiers.get(idx), cfg.updateHealth);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        LivingEntity entity = holder.owner();
        for (int idx = 0; idx < cfg.modifiers.size(); idx++) {
            removeOne(entity, powerId, idx, cfg.modifiers.get(idx), cfg.updateHealth);
            APPLIED.remove(new TrackKey(entity.getUUID(), powerId, idx));
        }
    }

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        LivingEntity entity = holder.owner();
        for (int idx = 0; idx < cfg.modifiers.size(); idx++) {
            reapplyOne(entity, holder, powerId, idx, cfg.modifiers.get(idx), cfg.updateHealth);
        }
    }

    private void reapplyOne(LivingEntity entity, PowerContainer holder, ResourceLocation powerId, int idx,
                            AttributeModifier modifier, boolean updateHealth) {
        Optional<ResourceLocation> attrId = modifier.attribute();
        if (attrId.isEmpty()) return;
        Attribute attr = BuiltInRegistries.ATTRIBUTE.get(attrId.get());
        if (attr == null) return;
        AttributeInstance instance = entity.getAttribute(attr);
        if (instance == null) return;

        TrackKey key = new TrackKey(entity.getUUID(), powerId, idx);
        java.util.UUID uuid = uuidFor(powerId, idx);
        boolean affectsMaxHealth = MAX_HEALTH_ID.equals(attrId.get().toString());
        float prevMax = affectsMaxHealth ? entity.getMaxHealth() : 0f;
        float prevHealth = affectsMaxHealth ? entity.getHealth() : 0f;
        boolean changed = false;

        if (!conditionHolds(entity, powerId)) {
            if (APPLIED.remove(key) != null) {
                instance.removeModifier(uuid);
                changed = true;
            }
        } else {
            Map<String, Double> vars = ExpressionContext.entityStats(entity);
            Map<String, Double> resources = ExpressionContext.resourceValues(holder);
            double newValue = modifier.resolveInput(vars, resources);

            Double last = APPLIED.get(key);
            if (last == null || Math.abs(last - newValue) >= 1.0e-6) {
                instance.removeModifier(uuid);
                String name = modifier.name().orElseGet(() -> "apoli:" + powerId);
                net.minecraft.world.entity.ai.attributes.AttributeModifier vanillaMod =
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        uuid, name, newValue, modifier.operation().vanillaOperation()
                    );
                instance.addTransientModifier(vanillaMod);
                APPLIED.put(key, newValue);
                changed = true;
            }
        }

        if (changed && affectsMaxHealth) {
            reconcileHealth(entity, prevMax, prevHealth, updateHealth);
        }
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
            player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), java.util.List.of(maxHealth)));
        }
        player.connection.send(new ClientboundSetHealthPacket(
            player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
    }

    private static boolean conditionHolds(LivingEntity entity, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || loaded.condition().isEmpty()) return true;
        if (!(entity.level() instanceof ServerLevel level)) return true;
        return loaded.condition().get().test(new EntityCtx(entity, level));
    }

    private void removeOne(LivingEntity entity, ResourceLocation powerId, int idx, AttributeModifier modifier, boolean updateHealth) {
        Optional<ResourceLocation> attrId = modifier.attribute();
        if (attrId.isEmpty()) return;
        Attribute attr = BuiltInRegistries.ATTRIBUTE.get(attrId.get());
        if (attr == null) return;
        AttributeInstance instance = entity.getAttribute(attr);
        if (instance == null) return;
        boolean affectsMaxHealth = MAX_HEALTH_ID.equals(attrId.get().toString());
        float prevMax = affectsMaxHealth ? entity.getMaxHealth() : 0f;
        float prevHealth = affectsMaxHealth ? entity.getHealth() : 0f;
        instance.removeModifier(uuidFor(powerId, idx));
        if (affectsMaxHealth) {
            reconcileHealth(entity, prevMax, prevHealth, updateHealth);
        }
    }

    private static java.util.UUID uuidFor(ResourceLocation powerId, int idx) {
        String seed = "apoli:attribute:" + powerId + ":" + idx;
        return java.util.UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
