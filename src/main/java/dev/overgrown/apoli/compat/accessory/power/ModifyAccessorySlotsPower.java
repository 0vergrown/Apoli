package dev.overgrown.apoli.compat.accessory.power;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.SlotModifier;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;

public final class ModifyAccessorySlotsPower extends PowerType<ModifyAccessorySlotsPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_accessory_slots");

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
        LivingEntity entity = holder.owner();
        if (entity == null || entity.level().isClientSide()) return;
        Accessories.applySlotModifiers(entity, build(cfg));
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        LivingEntity entity = holder.owner();
        if (entity == null || entity.level().isClientSide()) return;
        Accessories.removeSlotModifiers(entity, build(cfg));
    }
}
