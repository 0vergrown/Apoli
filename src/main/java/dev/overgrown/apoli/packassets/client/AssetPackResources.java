package dev.overgrown.apoli.packassets.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// A resource pack made of the downloaded data pack assets, read straight from the cache folder.
final class AssetPackResources implements PackResources {
    private final PackLocationInfo location;
    private final Map<ResourceLocation, Path> files;
    private final Set<String> namespaces = new HashSet<>();

    AssetPackResources(PackLocationInfo location, Map<ResourceLocation, Path> files) {
        this.location = location;
        this.files = files;
        for (ResourceLocation id : files.keySet()) namespaces.add(id.getNamespace());
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... segments) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        Path file = type == PackType.CLIENT_RESOURCES ? files.get(id) : null;
        return file == null ? null : IoSupplier.create(file);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !namespaces.contains(namespace)) return;
        String start = prefix + "/";
        for (Map.Entry<ResourceLocation, Path> entry : files.entrySet()) {
            ResourceLocation id = entry.getKey();
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(start)) {
                output.accept(id, IoSupplier.create(entry.getValue()));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? namespaces : Set.of();
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> reader) {
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {}
}
