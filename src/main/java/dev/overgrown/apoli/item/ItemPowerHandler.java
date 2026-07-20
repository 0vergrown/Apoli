package dev.overgrown.apoli.item;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

public final class ItemPowerHandler {
    private ItemPowerHandler() {}

    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();

    private static ResourceLocation sourceFor(EquipmentSlot slot) {
        return Apoli.id("item/" + slot.getSerializedName());
    }

    public static ItemStack[] newSlotArray() {
        return new ItemStack[SLOTS.length];
    }

    public static boolean anyEquippedPowers(LivingEntity entity) {
        for (EquipmentSlot slot : SLOTS) {
            if (ItemPowers.read(entity.getItemBySlot(slot.vanilla())) != null) return true;
        }
        return false;
    }

    public static boolean reconcile(LivingEntity entity, ItemStack @Nullable [] lastStacks) {
        PowerContainer container = PowerContainerAttachment.getOrCreate(entity);
        if (container == null) return false;

        boolean any = false;
        for (int i = 0; i < SLOTS.length; i++) {
            EquipmentSlot slot = SLOTS[i];
            ItemStack stack = entity.getItemBySlot(slot.vanilla());
            Set<ResourceLocation> desired = ItemPowers.powerIdsForSlot(stack, slot);
            ResourceLocation source = sourceFor(slot);
            Set<ResourceLocation> current = currentFromSource(container, source);
            ItemStack previous = lastStacks == null ? null : lastStacks[i];

            for (ResourceLocation p : current) {
                if (!desired.contains(p)) {
                    OptionalInt value = container.getAuxInt(p);
                    if (value.isPresent() && previous != null) {
                        ItemPowers.saveValue(previous, p, slot, value.getAsInt());
                    }
                    container.removePower(p, source);
                }
            }
            for (ResourceLocation p : desired) {
                if (!current.contains(p)) {
                    boolean hadAux = container.getAuxInt(p).isPresent();
                    container.addPower(p, source);

                    if (!hadAux && container instanceof PowerContainerImpl impl) {
                        OptionalInt saved = ItemPowers.readValue(stack, p, slot);
                        if (saved.isPresent()) impl.setAuxInt(p, saved.getAsInt());
                    }
                }
            }
            if (lastStacks != null) lastStacks[i] = stack;
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
