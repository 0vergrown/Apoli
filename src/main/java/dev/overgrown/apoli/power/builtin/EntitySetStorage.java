package dev.overgrown.apoli.power.builtin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class EntitySetStorage extends SavedData {
    private static final String NAME = "apoli_entity_sets";
    private static final Factory<EntitySetStorage> FACTORY = new Factory<>(EntitySetStorage::new, EntitySetStorage::load, null);

    private EntitySetStorage() {}

    public static void attach(MinecraftServer server) {
        EntitySetStorage storage = server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
        EntitySetPower.attachStorage(storage::setDirty);
    }

    private static EntitySetStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        EntitySetPower.loadPersistent(tag);
        return new EntitySetStorage();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return EntitySetPower.savePersistent(tag);
    }
}
