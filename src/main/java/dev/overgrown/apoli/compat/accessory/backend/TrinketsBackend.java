package dev.overgrown.apoli.compat.accessory.backend;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessoryProvider;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.compat.accessory.AccessorySlotRef;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;


public final class TrinketsBackend implements AccessoryProvider {
    public static final String ID = "trinkets";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isPresent() {
        return true; 
    }

    @Override
    public List<AccessorySlotRef> slots(LivingEntity entity) {
        return TrinketsApi.getTrinketComponent(entity).map(comp -> {
            List<AccessorySlotRef> out = new ArrayList<>();
            comp.getInventory().forEach((group, byName) -> byName.forEach((name, inv) -> {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    out.add(new Ref(new SlotReference(inv, i)));
                }
            }));
            return out;
        }).orElseGet(List::of);
    }

    @Override
    public List<AccessorySlotRef> equipped(LivingEntity entity) {
        
        List<AccessorySlotRef> out = new ArrayList<>();
        for (AccessorySlotRef ref : slots(entity)) {
            if (!ref.getStack().isEmpty()) out.add(ref);
        }
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
        return TrinketsApi.getTrinket(stack.getItem()) != TrinketsApi.getDefaultTrinket();
    }

    @Override
    public void applySlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {
        TrinketsApi.getTrinketComponent(entity).ifPresent(component -> component.addTemporaryModifiers(modifiers));
    }

    @Override
    public void removeSlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {
        TrinketsApi.getTrinketComponent(entity).ifPresent(component -> component.removeModifiers(modifiers));
    }

    
    public static AccessorySlotRef slotRef(SlotReference reference) {
        return new Ref(reference);
    }

    
    public static void handleChange(TrinketInventory inventory, int slot, ItemStack oldStack, ItemStack newStack) {
        TrinketComponent comp = inventory.getComponent();
        if (comp == null) return;
        LivingEntity entity = comp.getEntity();
        if (entity == null || entity.level().isClientSide()) return;
        Ref ref = new Ref(new SlotReference(inventory, slot));
        if (!oldStack.isEmpty()) Accessories.fireChange(entity, ref, oldStack, false);
        if (!newStack.isEmpty()) Accessories.fireChange(entity, ref, newStack, true);
    }

    private record Ref(SlotReference ref) implements AccessorySlotRef {
        @Override public String provider() { return ID; }
        @Override public String group() { return ref.inventory().getSlotType().getGroup(); }
        @Override public String type() { return ref.inventory().getSlotType().getName(); }
        @Override public int index() { return ref.index(); }
        @Override public ItemStack getStack() { return ref.inventory().getItem(ref.index()); }
        @Override public void setStack(ItemStack stack) { ref.inventory().setItem(ref.index(), stack); }
    }
}
