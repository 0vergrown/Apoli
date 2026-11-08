package dev.overgrown.apoli.compat.accessory;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class Accessories {
    private Accessories() {}

    @FunctionalInterface
    public interface ChangeListener {
        void onChange(LivingEntity entity, AccessorySlotRef ref, ItemStack stack, boolean equipping);
    }

    private static final List<AccessoryProvider> PROVIDERS = new ArrayList<>();
    private static ChangeListener changeListener = (e, r, s, eq) -> {};

    public static void register(AccessoryProvider provider) {
        if (provider != null && provider.isPresent()) PROVIDERS.add(provider);
    }

    public static boolean anyPresent() {
        return !PROVIDERS.isEmpty();
    }

    public static List<AccessoryProvider> providers() {
        return PROVIDERS;
    }

    public static void setChangeListener(ChangeListener listener) {
        changeListener = listener != null ? listener : (e, r, s, eq) -> {};
    }

    public static void fireChange(LivingEntity entity, AccessorySlotRef ref, ItemStack stack, boolean equipping) {
        changeListener.onChange(entity, ref, stack, equipping);
    }

    public static List<AccessorySlotRef> equipped(LivingEntity entity) {
        if (PROVIDERS.isEmpty()) return List.of();
        List<AccessorySlotRef> out = new ArrayList<>();
        for (AccessoryProvider p : PROVIDERS) out.addAll(p.equipped(entity));
        return out;
    }

    public static List<AccessorySlotRef> equipped(LivingEntity entity, List<AccessorySlot> filter) {
        List<AccessorySlotRef> out = new ArrayList<>();
        for (AccessorySlotRef ref : equipped(entity)) {
            if (AccessorySlot.matchesAny(filter, ref)) out.add(ref);
        }
        return out;
    }

    public static int count(LivingEntity entity, List<AccessorySlot> filter, @Nullable Predicate<ItemStack> item, int limit) {
        if (limit <= 0) return 0;
        int count = 0;
        for (int i = 0, n = PROVIDERS.size(); i < n && count < limit; i++) {
            count += PROVIDERS.get(i).count(entity, filter, item, limit - count);
        }
        return count;
    }

    public static boolean anyEquipped(LivingEntity entity, List<AccessorySlot> filter, @Nullable Predicate<ItemStack> item) {
        return count(entity, filter, item, 1) > 0;
    }

    public static List<AccessorySlotRef> slots(LivingEntity entity) {
        if (PROVIDERS.isEmpty()) return List.of();
        List<AccessorySlotRef> out = new ArrayList<>();
        for (AccessoryProvider p : PROVIDERS) out.addAll(p.slots(entity));
        return out;
    }

    public static List<AccessorySlotRef> slots(LivingEntity entity, List<AccessorySlot> filter) {
        List<AccessorySlotRef> out = new ArrayList<>();
        for (AccessorySlotRef ref : slots(entity)) {
            if (AccessorySlot.matchesAny(filter, ref)) out.add(ref);
        }
        return out;
    }

    public static boolean isAccessory(ItemStack stack, Level level) {
        for (AccessoryProvider p : PROVIDERS) {
            if (p.isAccessory(stack, level)) return true;
        }
        return false;
    }

    public static void applySlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {
        for (AccessoryProvider p : PROVIDERS) p.applySlotModifiers(entity, modifiers);
    }

    public static void removeSlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {
        for (AccessoryProvider p : PROVIDERS) p.removeSlotModifiers(entity, modifiers);
    }
}
