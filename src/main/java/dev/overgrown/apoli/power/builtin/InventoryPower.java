package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.ContainerType;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class InventoryPower extends PowerType<InventoryPower.Config> {
    private static final Component DEFAULT_TITLE = Component.translatable("container.inventory");

    public record Config(
        Component title,
        ContainerType containerType,
        boolean dropOnDeath,
        Optional<ItemCondition> dropOnDeathFilter,
        boolean recoverable,
        Key key
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TextComponent.CODEC.optionalFieldOf("title", DEFAULT_TITLE).forGetter(Config::title),
            ContainerType.CODEC.optionalFieldOf("container_type", ContainerType.DROPPER).forGetter(Config::containerType),
            Codec.BOOL.optionalFieldOf("drop_on_death", false).forGetter(Config::dropOnDeath),
            ItemCondition.CODEC.optionalFieldOf("drop_on_death_filter").forGetter(Config::dropOnDeathFilter),
            Codec.BOOL.optionalFieldOf("recoverable", true).forGetter(Config::recoverable),
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Config::key)
        ).apply(i, Config::new));
    }

    public void open(ResourceLocation powerId, Config cfg, ServerPlayer player, PowerContainerImpl container) {
        HolderLookup.Provider registries = player.registryAccess();
        SimpleContainer inv = load(container.getAuxNbt(powerId), cfg.containerType().slots(), registries);
        inv.addListener(changed -> container.setAuxNbt(powerId, save(inv, registries)));
        player.openMenu(cfg.containerType().menuProvider(cfg.title(), inv));
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.allPowers().contains(powerId)) return;
        if (!(holder instanceof PowerContainerImpl impl)) return;

        CompoundTag stored = impl.getAuxNbt(powerId);
        if (stored != null && cfg.recoverable() && holder.owner() instanceof ServerPlayer player) {
            SimpleContainer inv = load(stored, cfg.containerType().slots(), player.registryAccess());
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
            }
        }
        impl.removeAux(powerId);
    }

    public void dropOnDeath(ResourceLocation powerId, Config cfg, LivingEntity dead, PowerContainerImpl container, ServerLevel level) {
        CompoundTag stored = container.getAuxNbt(powerId);
        if (stored == null) return;
        SimpleContainer inv = load(stored, cfg.containerType().slots(), level.registryAccess());
        boolean changed = false;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            if (cfg.dropOnDeathFilter().isPresent()
                && !cfg.dropOnDeathFilter().get().test(new ItemCtx(stack, level, dead))) continue;
            dead.spawnAtLocation(stack);
            inv.setItem(slot, ItemStack.EMPTY);
            changed = true;
        }
        if (changed) container.setAuxNbt(powerId, save(inv, level.registryAccess()));
    }

    public static SimpleContainer openContainer(LivingEntity holder, ResourceLocation powerId) {
        if (!(holder.level() instanceof ServerLevel level)) return null;
        if (!(PowerContainer.of(holder) instanceof PowerContainerImpl impl)) return null;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || !(loaded.config() instanceof Config cfg)) return null;
        return load(impl.getAuxNbt(powerId), cfg.containerType().slots(), level.registryAccess());
    }

    public static void saveContainer(LivingEntity holder, ResourceLocation powerId, SimpleContainer container) {
        if (holder.level() instanceof ServerLevel level
            && PowerContainer.of(holder) instanceof PowerContainerImpl impl) {
            impl.setAuxNbt(powerId, save(container, level.registryAccess()));
        }
    }

    private static SimpleContainer load(CompoundTag stored, int slots, HolderLookup.Provider registries) {
        SimpleContainer inv = new SimpleContainer(slots);
        if (stored != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(stored, items, registries);
            for (int i = 0; i < slots; i++) inv.setItem(i, items.get(i));
        }
        return inv;
    }

    private static CompoundTag save(SimpleContainer inv, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < inv.getContainerSize(); i++) items.set(i, inv.getItem(i));
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items, registries);
        return tag;
    }
}
