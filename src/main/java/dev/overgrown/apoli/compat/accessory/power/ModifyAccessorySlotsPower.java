package dev.overgrown.apoli.compat.accessory.power;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.SlotModifier;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ModifyAccessorySlotsPower extends PowerType<ModifyAccessorySlotsPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_accessory_slots");

    private static final Map<LivingEntity, Map<ResourceLocation, Config>> APPLIED = new WeakHashMap<>();

    public record Config(List<SlotModifier> modifiers) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            SlotModifier.LIST.fieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }

    private static Multimap<String, AttributeModifier> build(Config cfg) {
        Multimap<String, AttributeModifier> map = HashMultimap.create();
        for (SlotModifier m : cfg.modifiers()) {
            map.put(m.slot(), m.toVanilla());
        }
        return map;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        sync(powerId, cfg, holder);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        sync(powerId, cfg, holder);
    }

    @Override
    public void onSuppressed(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        sync(powerId, cfg, holder);
    }

    @Override
    public void onUnsuppressed(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        sync(powerId, cfg, holder);
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        sync(powerId, cfg, holder);
    }

    private static void sync(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity entity = holder.owner();
        if (entity == null || entity.level().isClientSide()) return;
        boolean wanted = holder.hasPower(powerId) && !holder.isSuppressed(powerId) && conditionHolds(entity, powerId);
        Map<ResourceLocation, Config> applied = APPLIED.get(entity);
        Config current = applied == null ? null : applied.get(powerId);
        if (wanted ? current == cfg : current == null) return;
        if (current != null) Accessories.removeSlotModifiers(entity, build(current));
        if (wanted) {
            Multimap<String, AttributeModifier> modifiers = build(cfg);
            Accessories.removeSlotModifiers(entity, modifiers);
            Accessories.applySlotModifiers(entity, modifiers);
            if (applied == null) {
                applied = new HashMap<>(2);
                APPLIED.put(entity, applied);
            }
            applied.put(powerId, cfg);
        } else {
            applied.remove(powerId);
            if (applied.isEmpty()) APPLIED.remove(entity);
        }
    }

    private static boolean conditionHolds(LivingEntity entity, ResourceLocation powerId) {
        Power power = ApoliPowers.get(powerId);
        if (power == null || power.condition().isEmpty()) return true;
        return power.condition().get().test(EntityCtx.of(entity, entity.level()));
    }
}
