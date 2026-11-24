package dev.overgrown.apoli.station;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class StationContainer extends SimpleContainer {
    private final UUID owner;
    private final ResourceLocation powerId;
    private int viewers;
    private boolean itemsChanged;
    private boolean valuesChanged;
    private int ticksSinceFlush;
    private boolean removed;

    protected StationContainer(UUID owner, ResourceLocation powerId, int slots) {
        super(slots);
        this.owner = owner;
        this.powerId = powerId;
    }

    public UUID owner() {
        return owner;
    }

    public ResourceLocation powerId() {
        return powerId;
    }

    public abstract ContainerData data();

    public abstract boolean idle(StationSettings settings);

    public abstract boolean check(StationCheck check);

    protected abstract void saveValues(CompoundTag tag);

    protected abstract void loadValues(CompoundTag tag);

    public boolean viewed() {
        return viewers > 0;
    }

    protected void markValuesChanged() {
        valuesChanged = true;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        itemsChanged = true;
    }

    @Override
    public boolean stillValid(Player player) {
        return !removed && player.isAlive() && player.getUUID().equals(owner);
    }

    @Override
    public void startOpen(Player player) {
        viewers++;
    }

    @Override
    public void stopOpen(Player player) {
        viewers = Math.max(0, viewers - 1);
        StationStorage.flush(this, player);
    }

    void markRemoved() {
        removed = true;
    }

    boolean shouldFlush(int interval) {
        if (itemsChanged) return true;
        if (!valuesChanged) return false;
        return ++ticksSinceFlush >= interval;
    }

    CompoundTag save() {
        itemsChanged = false;
        valuesChanged = false;
        ticksSinceFlush = 0;
        NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < getContainerSize(); i++) items.set(i, getItem(i));
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items);
        saveValues(tag);
        return tag;
    }

    void load(@Nullable CompoundTag tag) {
        if (tag != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);
            for (int i = 0; i < getContainerSize(); i++) setItem(i, items.get(i));
            loadValues(tag);
        }
        itemsChanged = false;
        valuesChanged = false;
    }
}
