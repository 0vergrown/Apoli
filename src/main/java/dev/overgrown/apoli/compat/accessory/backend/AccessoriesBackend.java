package dev.overgrown.apoli.compat.accessory.backend;

import com.google.common.collect.Multimap;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessoryProvider;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.compat.accessory.AccessorySlotRef;
import dev.overgrown.apoli.compat.accessory.power.PreventAccessoryEquipPower;
import dev.overgrown.apoli.compat.accessory.power.PreventAccessoryUnequipPower;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.events.AccessoryChangeCallback;
import io.wispforest.accessories.api.events.CanEquipCallback;
import io.wispforest.accessories.api.events.CanUnequipCallback;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class AccessoriesBackend implements AccessoryProvider {
    public static final String ID = "accessories";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    private static AccessoriesCapability capability(LivingEntity entity) {
        return AccessoriesCapability.get(entity);
    }

    @Override
    public List<AccessorySlotRef> equipped(LivingEntity entity) {
        AccessoriesCapability cap = capability(entity);
        if (cap == null) return List.of();
        List<AccessorySlotRef> out = new ArrayList<>();
        for (SlotEntryReference entry : cap.getAllEquipped()) {
            out.add(new Ref(entry.reference()));
        }
        return out;
    }

    @Override
    public List<AccessorySlotRef> slots(LivingEntity entity) {
        AccessoriesCapability cap = capability(entity);
        if (cap == null) return List.of();
        List<AccessorySlotRef> out = new ArrayList<>();
        cap.getContainers().forEach((name, container) -> {
            for (int i = 0; i < container.getSize(); i++) {
                out.add(new Ref(SlotReference.of(entity, name, i)));
            }
        });
        return out;
    }

    @Override
    public ItemStack equip(LivingEntity entity, List<AccessorySlot> filter, ItemStack stack) {
        for (AccessorySlotRef ref : slots(entity)) {
            if (!AccessorySlot.matchesAny(filter, ref)) continue;
            if (!ref.getStack().isEmpty()) continue;
            ref.setStack(stack.copy());
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public boolean isAccessory(ItemStack stack, Level level) {
        return AccessoriesAPI.isValidAccessory(stack, level);
    }

    @Override
    public void applySlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {
        AccessoriesCapability cap = capability(entity);
        if (cap != null) cap.addTransientSlotModifiers(modifiers);
    }

    @Override
    public void removeSlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {
        AccessoriesCapability cap = capability(entity);
        if (cap != null) cap.removeSlotModifiers(modifiers);
    }

    public void registerEvents() {
        AccessoryChangeCallback.EVENT.register((prevStack, currentStack, reference, stateChange) -> {
            LivingEntity entity = reference.entity();
            if (entity == null || entity.level().isClientSide()) return;
            Ref ref = new Ref(reference);
            if (prevStack != null && !prevStack.isEmpty()) Accessories.fireChange(entity, ref, prevStack, false);
            if (currentStack != null && !currentStack.isEmpty()) Accessories.fireChange(entity, ref, currentStack, true);
        });
        CanEquipCallback.EVENT.register((stack, reference) -> {
            LivingEntity entity = reference.entity();
            return entity != null && PreventAccessoryEquipPower.isPrevented(entity, new Ref(reference), stack)
                ? TriState.FALSE : TriState.DEFAULT;
        });
        CanUnequipCallback.EVENT.register((stack, reference) -> {
            LivingEntity entity = reference.entity();
            return entity != null && PreventAccessoryUnequipPower.isPrevented(entity, new Ref(reference), stack)
                ? TriState.FALSE : TriState.DEFAULT;
        });
    }

    private record Ref(SlotReference ref) implements AccessorySlotRef {

        @Override
        public String provider() {
            return ID;
        }

        @Override
        public String group() {
            return "";
        }

        @Override
        public String type() {
            return ref.slotName();
        }

        @Override
        public int index() {
            return ref.slot();
        }

        @Override
        public ItemStack getStack() {
            ItemStack s = ref.getStack();
            return s == null ? ItemStack.EMPTY : s;
        }

        @Override
        public void setStack(ItemStack stack) {
            ref.setStack(stack);
        }
    }
}
