package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.CommandStorage;

public record StorageTarget(ResourceLocation storage, String path, boolean nbt, boolean merge) {

    public static final MapCodec<StorageTarget> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        IdCodecs.ID.fieldOf("storage").forGetter(StorageTarget::storage),
        Codec.STRING.optionalFieldOf("path", "").forGetter(StorageTarget::path),
        Codec.BOOL.optionalFieldOf("nbt", true).forGetter(StorageTarget::nbt),
        Codec.BOOL.optionalFieldOf("merge", false).forGetter(StorageTarget::merge)
    ).apply(i, StorageTarget::new));

    public void write(MinecraftServer server, CompoundTag value) {
        if (server == null) return;
        CommandStorage storage = server.getCommandStorage();
        CompoundTag root = storage.get(this.storage);
        CompoundTag holder = root;
        String[] keys = split(this.path);
        for (int k = 0; k < keys.length - 1; k++) {
            Tag child = holder.get(keys[k]);
            if (child instanceof CompoundTag compound) {
                holder = compound;
            } else {
                CompoundTag created = new CompoundTag();
                holder.put(keys[k], created);
                holder = created;
            }
        }
        if (keys.length == 0) {
            if (!merge) {
                for (String key : java.util.List.copyOf(root.getAllKeys())) root.remove(key);
            }
            root.merge(value);
        } else {
            String leaf = keys[keys.length - 1];
            if (merge && holder.get(leaf) instanceof CompoundTag existing) {
                existing.merge(value);
            } else {
                holder.put(leaf, value);
            }
        }
        storage.set(this.storage, root);
    }

    public static CompoundTag read(MinecraftServer server, ResourceLocation storage, String path) {
        if (server == null) return null;
        CompoundTag current = server.getCommandStorage().get(storage);
        for (String key : split(path)) {
            Tag child = current.get(key);
            if (!(child instanceof CompoundTag compound)) return null;
            current = compound;
        }
        return current;
    }

    private static String[] split(String path) {
        if (path == null || path.isEmpty()) return new String[0];
        return path.split("\\.");
    }
}
