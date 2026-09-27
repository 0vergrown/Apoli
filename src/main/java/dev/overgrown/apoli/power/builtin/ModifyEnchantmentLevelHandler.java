package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ModifyEnchantmentLevelHandler {
    private ModifyEnchantmentLevelHandler() {}

    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();
    private static final Owner[] NONE = new Owner[0];

    private static final class Owner {
        final LivingEntity entity;
        final List<ModifyEnchantmentLevelPower.Config> active = new ArrayList<>(2);
        final List<Holder.Reference<Enchantment>> resolved = new ArrayList<>(2);
        final List<AttributeModifier> combined = new ArrayList<>(4);
        final List<Holder.Reference<Enchantment>> attributeTargets = new ArrayList<>(2);
        final List<Holder.Reference<Enchantment>> syncedTargets = new ArrayList<>(2);
        int[] syncedLevels = new int[0];
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
    private static @Nullable ItemStack hintStack;
    private static @Nullable Owner hintOwner;
    private static @Nullable Thread hintThread;

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
            for (Owner old : current) {
                if (find(next, old.entity) == null) restoreAttributes(old);
            }
            server = next;
        }
        Arrays.fill(scan, 0, scanned, null);
        endToolScope();
        clearHint();
        for (Owner owner : server) syncAttributes(owner);
    }

    public static void clearServer() {
        server = NONE;
        endToolScope();
        clearHint();
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

    public static void hintAttacker(ItemStack weapon, DamageSource source) {
        if (inactive()) return;
        Owner owner = source.getEntity() instanceof LivingEntity attacker ? stateOf(attacker) : null;
        if (owner == null) return;
        hintStack = weapon;
        hintOwner = owner;
        hintThread = Thread.currentThread();
    }

    public static int level(ItemStack stack, Holder<Enchantment> enchantment, int base) {
        if (inactive()) return base;
        Owner owner = resolve(stack);
        return owner == null ? base : level(owner, stack, enchantment, base);
    }

    public static ItemEnchantments enchantments(ItemStack stack, ItemEnchantments base) {
        if (inactive()) return base;
        Owner hinted = takeHint(stack);
        Owner owner = resolve(stack);
        if (owner == null) owner = hinted;
        return owner == null ? base : all(owner, stack, base);
    }

    public static @Nullable ItemEnchantments enchantmentsFor(LivingEntity holder, ItemStack stack, @Nullable ItemEnchantments base) {
        if (inactive()) return base;
        Owner owner = stateOf(holder);
        if (owner == null) return base;
        ItemEnchantments result = all(owner, stack, base == null ? ItemEnchantments.EMPTY : base);
        return base == null && result.isEmpty() ? null : result;
    }

    public static @Nullable ItemEnchantments enchantmentsUnowned(LivingEntity holder, ItemStack stack, @Nullable ItemEnchantments base) {
        if (inactive() || resolve(stack) != null) return base;
        return enchantmentsFor(holder, stack, base);
    }

    public static int emptySlotLevel(LivingEntity living, Holder<Enchantment> enchantment, int base) {
        if (inactive()) return base;
        Owner owner = stateOf(living);
        if (owner == null) return base;
        for (EquipmentSlot slot : SLOTS) {
            if (living.getItemBySlot(slot).isEmpty() && enchantment.value().matchingSlot(slot)) {
                return Math.max(base, level(owner, ItemStack.EMPTY, enchantment, 0));
            }
        }
        return base;
    }

    public static int levelInContext(@Nullable LivingEntity context, ItemStack stack, Holder<Enchantment> enchantment, int gameplayLevel) {
        if (inactive() || resolve(stack) != null) return gameplayLevel;
        Owner owner = context == null ? null : stateOf(context);
        return owner == null ? gameplayLevel : level(owner, stack, enchantment, gameplayLevel);
    }

    public static ItemEnchantments enchantmentsInContext(@Nullable LivingEntity context, ItemStack stack, ItemEnchantments raw) {
        if (inactive()) return raw;
        Owner owner = resolve(stack);
        if (owner == null && context != null) owner = stateOf(context);
        return owner == null ? raw : all(owner, stack, raw);
    }

    public static void onGetLevel(ItemStack stack, ItemEnchantments.Mutable enchantments, @Nullable Holder<Enchantment> target) {
        if (inactive()) return;
        Owner hinted = takeHint(stack);
        Owner owner = resolve(stack);
        if (owner == null) owner = hinted;
        if (owner == null) return;
        if (target != null) {
            int before = enchantments.getLevel(target);
            int after = level(owner, stack, target, before);
            if (after != before) enchantments.set(target, after);
            return;
        }
        if (owner.busy || !refresh(owner)) return;
        for (int i = 0; i < owner.resolved.size(); i++) {
            Holder.Reference<Enchantment> enchantment = owner.resolved.get(i);
            if (enchantment == null || seenBefore(owner, i, enchantment)) continue;
            int before = enchantments.getLevel(enchantment);
            int after = level(owner, stack, enchantment, before);
            if (after != before) enchantments.set(enchantment, after);
        }
    }

    private static ItemEnchantments all(Owner owner, ItemStack stack, ItemEnchantments base) {
        if (owner.busy || !refresh(owner)) return base;
        ItemEnchantments.Mutable out = null;
        for (int i = 0; i < owner.resolved.size(); i++) {
            Holder.Reference<Enchantment> enchantment = owner.resolved.get(i);
            if (enchantment == null || seenBefore(owner, i, enchantment)) continue;
            int before = base.getLevel(enchantment);
            int after = level(owner, stack, enchantment, before);
            if (after == before) continue;
            if (out == null) out = new ItemEnchantments.Mutable(base);
            out.set(enchantment, after);
        }
        return out == null ? base : out.toImmutable();
    }

    private static int level(Owner owner, ItemStack stack, Holder<Enchantment> enchantment, int base) {
        if (owner.busy || !refresh(owner)) return base;
        owner.busy = true;
        try {
            List<AttributeModifier> mods = null;
            for (int i = 0; i < owner.active.size(); i++) {
                ModifyEnchantmentLevelPower.Config cfg = owner.active.get(i);
                if (cfg.allModifiers().isEmpty() || !enchantment.is(cfg.key()) || !cfg.appliesTo(stack, owner.entity)) continue;
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
            owner.attributeTargets.clear();
            owner.busy = true;
            try {
                PowerLookup.collect(owner.entity, ApoliIds.MODIFY_ENCHANTMENT_LEVEL, ModifyEnchantmentLevelPower.Config.class, owner.active);
                Registry<Enchantment> registry = owner.entity.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                for (int i = 0; i < owner.active.size(); i++) {
                    owner.resolved.add(registry.getHolder(owner.active.get(i).key()).orElse(null));
                }
                collectAttributeTargets(owner, registry);
            } finally {
                owner.busy = false;
            }
        }
        return !owner.active.isEmpty();
    }

    private static void collectAttributeTargets(Owner owner, Registry<Enchantment> registry) {
        PowerContainer container = PowerContainer.of(owner.entity);
        if (container == null) return;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.MODIFY_ENCHANTMENT_LEVEL);
        for (int i = 0; i < powers.size(); i++) {
            Power power = ApoliPowers.get(powers.get(i));
            if (power == null || !(power.config() instanceof ModifyEnchantmentLevelPower.Config cfg)) continue;
            Holder.Reference<Enchantment> enchantment = registry.getHolder(cfg.key()).orElse(null);
            if (enchantment == null || owner.attributeTargets.contains(enchantment)) continue;
            if (enchantment.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES).isEmpty()) continue;
            owner.attributeTargets.add(enchantment);
        }
    }

    private static void syncAttributes(Owner owner) {
        LivingEntity entity = owner.entity;
        if (entity.isRemoved()) return;
        refresh(owner);
        List<Holder.Reference<Enchantment>> targets = owner.attributeTargets;
        if (!owner.syncedTargets.equals(targets)) {
            for (int i = 0; i < owner.syncedTargets.size(); i++) {
                Holder.Reference<Enchantment> dropped = owner.syncedTargets.get(i);
                if (!targets.contains(dropped)) restoreAttributes(entity, dropped);
            }
            owner.syncedTargets.clear();
            owner.syncedTargets.addAll(targets);
            owner.syncedLevels = new int[SLOTS.length * targets.size()];
            Arrays.fill(owner.syncedLevels, -1);
        }
        int count = targets.size();
        if (count == 0) return;
        boolean changed = false;
        for (int s = 0; s < SLOTS.length; s++) {
            ItemStack stack = entity.getItemBySlot(SLOTS[s]);
            ItemEnchantments raw = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (int t = 0; t < count; t++) {
                Holder<Enchantment> enchantment = targets.get(t);
                int level = level(owner, stack, enchantment, raw.getLevel(enchantment));
                int index = s * count + t;
                if (owner.syncedLevels[index] != level) {
                    owner.syncedLevels[index] = level;
                    changed = true;
                }
            }
        }
        if (!changed) return;
        AttributeMap attributes = entity.getAttributes();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            ItemEnchantments raw = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (int t = 0; t < count; t++) {
                Holder<Enchantment> enchantment = targets.get(t);
                applyAttributeEffects(attributes, enchantment, slot, level(owner, stack, enchantment, raw.getLevel(enchantment)));
            }
        }
    }

    private static void restoreAttributes(Owner owner) {
        for (int t = 0; t < owner.syncedTargets.size(); t++) restoreAttributes(owner.entity, owner.syncedTargets.get(t));
    }

    private static void restoreAttributes(LivingEntity entity, Holder<Enchantment> enchantment) {
        if (entity.isRemoved()) return;
        AttributeMap attributes = entity.getAttributes();
        for (EquipmentSlot slot : SLOTS) {
            ItemEnchantments raw = entity.getItemBySlot(slot).getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            applyAttributeEffects(attributes, enchantment, slot, raw.getLevel(enchantment));
        }
    }

    private static void applyAttributeEffects(AttributeMap attributes, Holder<Enchantment> enchantment, EquipmentSlot slot, int level) {
        if (!enchantment.value().matchingSlot(slot)) return;
        List<EnchantmentAttributeEffect> effects = enchantment.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES);
        for (int i = 0; i < effects.size(); i++) {
            EnchantmentAttributeEffect effect = effects.get(i);
            AttributeInstance instance = attributes.getInstance(effect.attribute());
            if (instance == null) continue;
            net.minecraft.world.entity.ai.attributes.AttributeModifier modifier = effect.getModifier(Math.max(level, 1), slot);
            instance.removeModifier(modifier.id());
            if (level > 0) instance.addTransientModifier(modifier);
        }
    }

    private static boolean seenBefore(Owner owner, int index, Holder<Enchantment> enchantment) {
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

    private static @Nullable Owner takeHint(ItemStack stack) {
        if (hintStack != stack || hintThread != Thread.currentThread()) return null;
        Owner owner = hintOwner;
        clearHint();
        return owner;
    }

    private static void clearHint() {
        hintStack = null;
        hintOwner = null;
        hintThread = null;
    }
}
