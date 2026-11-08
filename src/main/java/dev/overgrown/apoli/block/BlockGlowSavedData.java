package dev.overgrown.apoli.block;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public final class BlockGlowSavedData extends SavedData {

    private static final String NAME = "apoli_block_glows";

    private final ResourceKey<Level> dimension;
    private @Nullable ListTag loaded;

    private BlockGlowSavedData(ResourceKey<Level> dimension, @Nullable ListTag loaded) {
        this.dimension = dimension;
        this.loaded = loaded;
    }

    public static BlockGlowSavedData get(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(
            () -> new BlockGlowSavedData(dimension, null),
            (tag, registries) -> new BlockGlowSavedData(dimension, tag.getList("glows", Tag.TAG_COMPOUND)),
            null), NAME);
    }

    public @Nullable ListTag takeLoaded() {
        ListTag taken = loaded;
        loaded = null;
        return taken;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("glows", BlockGlows.save(dimension));
        return tag;
    }
}
