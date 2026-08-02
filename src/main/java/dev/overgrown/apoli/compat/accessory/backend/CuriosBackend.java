package dev.overgrown.apoli.compat.accessory.backend;

import com.google.common.collect.Multimap;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessoryProvider;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.compat.accessory.AccessorySlotRef;
import dev.overgrown.apoli.compat.accessory.power.PreventAccessoryEquipPower;
import dev.overgrown.apoli.compat.accessory.power.PreventAccessoryUnequipPower;
import dev.overgrown.apoli.item.ConjuredItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.TriState;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import top.theillusivec4.curios.api.event.CurioCanUnequipEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CuriosBackend implements AccessoryProvider {
    public static final String ID = "curios";

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
        return CuriosApi.getCuriosInventory(entity).map(handler -> {
            List<AccessorySlotRef> out = new ArrayList<>();
            for (Map.Entry<String, ICurioStacksHandler> e : handler.getCurios().entrySet()) {
                IDynamicStackHandler stacks = e.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    out.add(new Ref(e.getKey(), i, stacks));
                }
            }
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
        return CuriosApi.getCurio(stack).isPresent();
    }

    @Override
    public void applySlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> handler.addTransientSlotModifiers(modifiers));
    }

    @Override
    public void removeSlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> handler.removeSlotModifiers(modifiers));
    }

    public static void onCurioChange(CurioChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide()) return;
        Ref ref = refFor(entity, event.getIdentifier(), event.getSlotIndex());
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();
        if (!from.isEmpty()) Accessories.fireChange(entity, ref, from, false);
        if (!to.isEmpty()) Accessories.fireChange(entity, ref, to, true);
    }

    public static void onCanEquip(CurioCanEquipEvent event) {
        LivingEntity entity = event.getEntity();
        SlotContext ctx = event.getSlotContext();
        if (entity != null && PreventAccessoryEquipPower.isPrevented(entity, refFor(entity, ctx.identifier(), ctx.index()), event.getStack())) {
            event.setEquipResult(TriState.FALSE);
        }
    }

    public static void onCanUnequip(CurioCanUnequipEvent event) {
        if (ConjuredItems.isLocked(event.getStack())) {
            event.setUnequipResult(TriState.FALSE);
            return;
        }
        LivingEntity entity = event.getEntity();
        SlotContext ctx = event.getSlotContext();
        if (entity != null && PreventAccessoryUnequipPower.isPrevented(entity, refFor(entity, ctx.identifier(), ctx.index()), event.getStack())) {
            event.setUnequipResult(TriState.FALSE);
        }
    }

    private static Ref refFor(LivingEntity entity, String identifier, int index) {
        IDynamicStackHandler handler = CuriosApi.getCuriosInventory(entity)
            .map(h -> h.getCurios().get(identifier))
            .map(ICurioStacksHandler::getStacks)
            .orElse(null);
        return new Ref(identifier, index, handler);
    }

    private record Ref(String identifier, int index, IDynamicStackHandler handler) implements AccessorySlotRef {
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
            return identifier;
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public ItemStack getStack() {
            return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(index);
        }

        @Override
        public void setStack(ItemStack stack) {
            if (handler != null) handler.setStackInSlot(index, stack);
        }
    }
}
