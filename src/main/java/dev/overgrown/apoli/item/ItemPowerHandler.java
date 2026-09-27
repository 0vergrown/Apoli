package dev.overgrown.apoli.item;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.access.ItemPowerSlots;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

public final class ItemPowerHandler {
    private ItemPowerHandler() {}

    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();
    private static final ResourceLocation[] SOURCES = new ResourceLocation[SLOTS.length];

    static {
        for (int i = 0; i < SLOTS.length; i++) SOURCES[i] = Apoli.id("item/" + SLOTS[i].getSerializedName());
    }

    public static ItemStack[] newSlotArray() {
        return new ItemStack[SLOTS.length];
    }

    public static void onLoad(LivingEntity entity) {
        if (entity.level().isClientSide() || !(entity instanceof ItemPowerSlots slots)) return;
        ItemStack[] lastStacks = slots.apoli$itemPowerStacks();
        for (int i = 0; i < SLOTS.length; i++) reconcileSlot(entity, i, lastStacks);
    }

    public static void onEquipmentChange(LivingEntity entity, net.minecraft.world.entity.EquipmentSlot vanillaSlot) {
        if (entity.level().isClientSide() || !(entity instanceof ItemPowerSlots slots)) return;
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i].vanilla() == vanillaSlot) {
                reconcileSlot(entity, i, slots.apoli$itemPowerStacks());
                return;
            }
        }
    }

    private static void reconcileSlot(LivingEntity entity, int index, ItemStack[] lastStacks) {
        EquipmentSlot slot = SLOTS[index];
        ItemStack stack = entity.getItemBySlot(slot.vanilla());
        ItemStack previous = lastStacks[index];
        lastStacks[index] = stack;
        Set<ResourceLocation> desired = ItemPowers.powerIdsForSlot(stack, slot);
        PowerContainer container = desired.isEmpty()
            ? PowerContainer.of(entity)
            : PowerContainerAttachment.getOrCreate(entity);
        if (container == null || container.isEmpty() && desired.isEmpty()) return;

        ResourceLocation source = SOURCES[index];
        Set<ResourceLocation> current = currentFromSource(container, source);
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
