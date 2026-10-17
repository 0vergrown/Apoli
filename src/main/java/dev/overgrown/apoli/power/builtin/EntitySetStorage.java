package dev.overgrown.apoli.power.builtin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class EntitySetStorage extends SavedData {
    private static final String NAME = "apoli_entity_sets";

    private EntitySetStorage() {}

    public static void attach(MinecraftServer server) {
        EntitySetStorage storage = server.overworld().getDataStorage().computeIfAbsent(EntitySetStorage::load, EntitySetStorage::new, NAME);
        EntitySetPower.attachStorage(storage::setDirty);
    }

    private static EntitySetStorage load(CompoundTag tag) {
        EntitySetPower.loadPersistent(tag);
        return new EntitySetStorage();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        return EntitySetPower.savePersistent(tag);
    }
}
