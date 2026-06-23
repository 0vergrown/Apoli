package dev.overgrown.apoli.item;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public final class ItemPowerHandler {
    private ItemPowerHandler() {}

    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();

    private static ResourceLocation sourceFor(EquipmentSlot slot) {
        return Apoli.id("item/" + slot.getSerializedName());
    }

    public static boolean anyEquippedPowers(LivingEntity entity) {
        for (EquipmentSlot slot : SLOTS) {
            if (ItemPowers.read(entity.getItemBySlot(slot.vanilla())) != null) return true;
        }
        return false;
    }

    public static boolean reconcile(LivingEntity entity) {
        PowerContainer container = PowerContainerAttachment.getOrCreate(entity);
        if (container == null) return false;

        boolean any = false;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot.vanilla());
            Set<ResourceLocation> desired = ItemPowers.powerIdsForSlot(stack, slot);
            ResourceLocation source = sourceFor(slot);
            Set<ResourceLocation> current = currentFromSource(container, source);

            for (ResourceLocation p : current) {
                if (!desired.contains(p)) container.removePower(p, source);
            }
            for (ResourceLocation p : desired) {
                if (!current.contains(p)) container.addPower(p, source);
            }
            if (!desired.isEmpty()) any = true;
        }
        return any;
    }

    private static Set<ResourceLocation> currentFromSource(PowerContainer container, ResourceLocation source) {
        Set<ResourceLocation> out = null;
        for (ResourceLocation p : container.allPowers()) {
            if (container.sourcesOf(p).contains(source)) {
                if (out == null) out = new HashSet<>();
                out.add(p);
            }
        }
        return out == null ? Set.of() : out;
    }
}
