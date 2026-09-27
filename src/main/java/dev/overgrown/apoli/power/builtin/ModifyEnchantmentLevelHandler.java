package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.mixin.enchantment.EnchantmentSlotsAccessor;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModifyEnchantmentLevelHandler {
    private ModifyEnchantmentLevelHandler() {}

    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();
    private static final Owner[] NONE = new Owner[0];

    private static final class Owner {
        final LivingEntity entity;
        final List<ModifyEnchantmentLevelPower.Config> active = new ArrayList<>(2);
        final List<Enchantment> resolved = new ArrayList<>(2);
        final List<AttributeModifier> combined = new ArrayList<>(4);
        long tick = Long.MIN_VALUE;
        boolean busy;

        Owner(LivingEntity entity) {
            this.entity = entity;
        }
    }

    private static Owner[] server = NONE;
    private static @Nullable Owner client;
    private static LivingEntity[] scan = new LivingEntity[4];
    private static int scanned;
    private static @Nullable ItemStack scopeStack;
    private static @Nullable Owner scopeOwner;
    private static @Nullable Thread scopeThread;

    public static void beginScan() {
        scanned = 0;
    }

    public static void scan(Entity entity, PowerContainer container) {
        if (!(entity instanceof LivingEntity living)) return;
        if (container.powersOfType(ApoliIds.MODIFY_ENCHANTMENT_LEVEL).isEmpty()) return;
        if (scanned == scan.length) scan = Arrays.copyOf(scan, scanned * 2);
        scan[scanned++] = living;
    }

    public static void endScan() {
        Owner[] current = server;
        boolean same = current.length == scanned;
        for (int i = 0; same && i < scanned; i++) same = current[i].entity == scan[i];
        if (!same) {
            Owner[] next = scanned == 0 ? NONE : new Owner[scanned];
            for (int i = 0; i < scanned; i++) {
                Owner kept = find(current, scan[i]);
                next[i] = kept != null ? kept : new Owner(scan[i]);
            }
            server = next;
        }
        Arrays.fill(scan, 0, scanned, null);
        endToolScope();
    }

    public static void clearServer() {
        server = NONE;
        endToolScope();
    }

    public static void clientTick(@Nullable LivingEntity player) {
        if (player == null) {
            client = null;
            return;
        }
        PowerContainer container = PowerContainer.of(player);
        if (container == null || container.powersOfType(ApoliIds.MODIFY_ENCHANTMENT_LEVEL).isEmpty()) {
            client = null;
        } else if (client == null || client.entity != player) {
            client = new Owner(player);
        }
    }

    public static boolean inactive() {
        return server.length == 0 && client == null;
    }

    public static boolean isHolder(LivingEntity entity) {
        return !inactive() && stateOf(entity) != null;
    }

    public static int level(ItemStack stack, Enchantment enchantment, int base) {
        if (inactive()) return base;
        Owner owner = resolve(stack);
        return owner == null ? base : level(owner, stack, enchantment, base);
    }

    public static ListTag enchantmentTags(ItemStack stack, ListTag base) {
        if (inactive()) return base;
        Owner owner = resolve(stack);
        return owner == null ? base : tags(owner, stack, base);
    }

    public static int emptySlotLevel(LivingEntity living, Enchantment enchantment, int base) {
        if (inactive()) return base;
        Owner owner = stateOf(living);
        if (owner == null) return base;
        for (EquipmentSlot slot : ((EnchantmentSlotsAccessor) enchantment).apoli$getSlots()) {
            if (living.getItemBySlot(slot).isEmpty()) {
                return Math.max(base, level(owner, ItemStack.EMPTY, enchantment, 0));
            }
        }
        return base;
    }

    public static float emptyDamageBonus(LivingEntity living, MobType mobType) {
        if (inactive()) return 0.0F;
        Owner owner = stateOf(living);
        if (owner == null || owner.busy || !refresh(owner)) return 0.0F;
        float bonus = 0.0F;
        for (int i = 0; i < owner.resolved.size(); i++) {
            Enchantment enchantment = owner.resolved.get(i);
            if (enchantment == null || seenBefore(owner, i, enchantment)) continue;
            int level = level(owner, ItemStack.EMPTY, enchantment, 0);
            if (level > 0) bonus += enchantment.getDamageBonus(level, mobType);
        }
        return bonus;
    }

    public static int levelInContext(@Nullable LivingEntity context, ItemStack stack, Enchantment enchantment, int gameplayLevel) {
        if (inactive() || resolve(stack) != null) return gameplayLevel;
        Owner owner = context == null ? null : stateOf(context);
        return owner == null ? gameplayLevel : level(owner, stack, enchantment, gameplayLevel);
    }

    public static Map<Enchantment, Integer> enchantmentsInContext(@Nullable LivingEntity context, ItemStack stack, Map<Enchantment, Integer> raw) {
        if (inactive()) return raw;
        Owner owner = resolve(stack);
        if (owner == null && context != null) owner = stateOf(context);
        if (owner == null || owner.busy || !refresh(owner)) return raw;
        Map<Enchantment, Integer> out = null;
        for (int i = 0; i < owner.resolved.size(); i++) {
            Enchantment enchantment = owner.resolved.get(i);
            if (enchantment == null || seenBefore(owner, i, enchantment)) continue;
            int before = raw.getOrDefault(enchantment, 0);
            int after = level(owner, stack, enchantment, before);
            if (after == before) continue;
            if (out == null) out = new LinkedHashMap<>(raw);
            if (after > 0) out.put(enchantment, after);
            else out.remove(enchantment);
        }
        return out == null ? raw : out;
    }

    public static int rawLevel(ItemStack stack, Enchantment enchantment) {
        if (stack.isEmpty()) return 0;
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        ListTag list = stack.getEnchantmentTags();
        int index = id == null ? -1 : indexOf(list, id);
        return index < 0 ? 0 : EnchantmentHelper.getEnchantmentLevel(list.getCompound(index));
    }

    public static boolean matchesSlot(Enchantment enchantment, EquipmentSlot slot) {
        for (EquipmentSlot candidate : ((EnchantmentSlotsAccessor) enchantment).apoli$getSlots()) {
            if (candidate == slot) return true;
        }
        return false;
    }

    private static ListTag tags(Owner owner, ItemStack stack, ListTag base) {
        if (owner.busy || !refresh(owner)) return base;
        ListTag out = null;
        for (int i = 0; i < owner.resolved.size(); i++) {
            Enchantment enchantment = owner.resolved.get(i);
            if (enchantment == null || seenBefore(owner, i, enchantment)) continue;
            ResourceLocation id = owner.active.get(i).enchantment();
            ListTag list = out == null ? base : out;
            int index = indexOf(list, id);
            int before = index < 0 ? 0 : EnchantmentHelper.getEnchantmentLevel(list.getCompound(index));
            int after = level(owner, stack, enchantment, before);
            if (after == before) continue;
            if (out == null) out = base.copy();
            if (after <= 0) {
                if (index >= 0) out.remove(index);
            } else if (index >= 0) {
                EnchantmentHelper.setEnchantmentLevel(out.getCompound(index), after);
            } else {
                out.add(EnchantmentHelper.storeEnchantment(id, after));
            }
        }
        return out == null ? base : out;
    }

    private static int indexOf(ListTag list, ResourceLocation id) {
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(EnchantmentHelper.getEnchantmentId(list.getCompound(i)))) return i;
        }
        return -1;
    }

    private static int level(Owner owner, ItemStack stack, Enchantment enchantment, int base) {
        if (owner.busy || !refresh(owner)) return base;
        owner.busy = true;
        try {
            List<AttributeModifier> mods = null;
            for (int i = 0; i < owner.active.size(); i++) {
                ModifyEnchantmentLevelPower.Config cfg = owner.active.get(i);
                if (cfg.allModifiers().isEmpty() || owner.resolved.get(i) != enchantment || !cfg.appliesTo(stack, owner.entity)) continue;
                if (mods == null) {
                    mods = cfg.allModifiers();
                } else {
                    if (mods != owner.combined) {
                        owner.combined.clear();
                        owner.combined.addAll(mods);
                        mods = owner.combined;
                    }
                    owner.combined.addAll(cfg.allModifiers());
                }
            }
            if (mods == null) return base;
            return Mth.clamp((int) Math.round(AttributeModifierHelper.apply((double) base, mods, owner.entity)), 0, 255);
        } finally {
            owner.busy = false;
        }
    }

    private static boolean refresh(Owner owner) {
        long now = owner.entity.level().getGameTime();
        if (owner.tick != now) {
            owner.tick = now;
            owner.active.clear();
            owner.resolved.clear();
            owner.busy = true;
            try {
                PowerLookup.collect(owner.entity, ApoliIds.MODIFY_ENCHANTMENT_LEVEL, ModifyEnchantmentLevelPower.Config.class, owner.active);
                for (int i = 0; i < owner.active.size(); i++) {
                    owner.resolved.add(BuiltInRegistries.ENCHANTMENT.getOptional(owner.active.get(i).enchantment()).orElse(null));
                }
            } finally {
                owner.busy = false;
            }
        }
        return !owner.active.isEmpty();
    }

    private static boolean seenBefore(Owner owner, int index, Enchantment enchantment) {
        for (int j = 0; j < index; j++) {
            if (owner.resolved.get(j) == enchantment) return true;
        }
        return false;
    }

    public static boolean beginToolScope(@Nullable Entity entity, ItemStack tool) {
        if (inactive() || scopeThread != null || !(entity instanceof LivingEntity living)) return false;
        Owner owner = stateOf(living);
        if (owner == null) return false;
        scopeStack = tool;
        scopeOwner = owner;
        scopeThread = Thread.currentThread();
        return true;
    }

    public static void endToolScope() {
        scopeStack = null;
        scopeOwner = null;
        scopeThread = null;
    }

    public static boolean inToolScope(ItemStack stack) {
        return scopeStack == stack && scopeThread == Thread.currentThread();
    }

    private static @Nullable Owner resolve(ItemStack stack) {
        Owner owner = ownerOf(stack);
        if (owner == null && inToolScope(stack)) owner = scopeOwner;
        return owner;
    }

    private static @Nullable Owner ownerOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Owner[] owners = server;
        for (Owner owner : owners) {
            if (wears(owner.entity, stack)) return owner;
        }
        Owner local = client;
        return local != null && wears(local.entity, stack) ? local : null;
    }

    private static @Nullable Owner stateOf(LivingEntity entity) {
        Owner[] owners = server;
        for (Owner owner : owners) {
            if (owner.entity == entity) return owner;
        }
        Owner local = client;
        return local != null && local.entity == entity ? local : null;
    }

    private static @Nullable Owner find(Owner[] owners, LivingEntity entity) {
        for (Owner owner : owners) {
            if (owner.entity == entity) return owner;
        }
        return null;
    }

    private static boolean wears(LivingEntity entity, ItemStack stack) {
        if (entity.isRemoved()) return false;
        for (EquipmentSlot slot : SLOTS) {
            if (entity.getItemBySlot(slot) == stack) return true;
        }
        return false;
    }
}
