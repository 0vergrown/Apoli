package dev.overgrown.apoli.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class JsonDataFiles {
    private JsonDataFiles() {}

    public record Source(ResourceLocation file, Resource resource, JsonFormat format) {
        public String packId() {
            return resource.sourcePackId();
        }
    }

    public record Version(Source source, JsonElement json) {}

    public static Map<ResourceLocation, JsonElement> load(ResourceManager manager, String directory, Logger log) {
        Map<ResourceLocation, Source> sources = topSources(manager, directory, log);
        Map<ResourceLocation, JsonElement> loaded = new HashMap<>(sources.size());
        for (Map.Entry<ResourceLocation, Source> entry : sources.entrySet()) {
            JsonElement json = read(entry.getValue(), entry.getKey(), log);
            if (json != null) loaded.put(entry.getKey(), json);
        }
        return loaded;
    }

    public static Map<ResourceLocation, Source> topSources(ResourceManager manager, String directory, Logger log) {
        Map<ResourceLocation, Resource> files = manager.listResources(directory, JsonDataFiles::isDataFile);
        Map<ResourceLocation, Source> top = new HashMap<>(files.size());
        PackOrder order = null;
        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            ResourceLocation file = entry.getKey();
            JsonFormat format = JsonFormat.of(file.getPath());
            ResourceLocation id = idOf(file, directory, format);
            if (id == null) continue;
            Source source = new Source(file, entry.getValue(), format);
            Source previous = top.putIfAbsent(id, source);
            if (previous == null) continue;
            if (order == null) order = new PackOrder(manager);
            if (order.compare(source, previous, id, log) > 0) top.put(id, source);
        }
        return top;
    }

    public static Map<ResourceLocation, List<Source>> stacks(ResourceManager manager, String directory, Logger log) {
        Map<ResourceLocation, List<Resource>> files = manager.listResourceStacks(directory, JsonDataFiles::isDataFile);
        Map<ResourceLocation, List<Source>> stacks = new HashMap<>(files.size());
        PackOrder order = null;
        for (Map.Entry<ResourceLocation, List<Resource>> entry : files.entrySet()) {
            ResourceLocation file = entry.getKey();
            JsonFormat format = JsonFormat.of(file.getPath());
            ResourceLocation id = idOf(file, directory, format);
            if (id == null) continue;
            List<Resource> resources = entry.getValue();
            List<Source> sources = new ArrayList<>(resources.size());
            for (int i = 0, n = resources.size(); i < n; i++) {
                sources.add(new Source(file, resources.get(i), format));
            }
            List<Source> previous = stacks.putIfAbsent(id, sources);
            if (previous == null) continue;
            if (order == null) order = new PackOrder(manager);
            stacks.put(id, order.merge(previous, sources, id, log));
        }
        return stacks;
    }

    public static Map<ResourceLocation, Version> loadByPriority(ResourceManager manager, String directory, Logger log) {
        Map<ResourceLocation, List<Source>> stacks = stacks(manager, directory, log);
        Map<ResourceLocation, Version> chosen = new HashMap<>(stacks.size());
        for (Map.Entry<ResourceLocation, List<Source>> entry : stacks.entrySet()) {
            ResourceLocation id = entry.getKey();
            List<Source> sources = entry.getValue();
            Version best = null;
            int bestPriority = 0;
            for (int i = 0, n = sources.size(); i < n; i++) {
                Source source = sources.get(i);
                JsonElement json = read(source, id, log);
                if (json == null) continue;
                int priority = n == 1 ? 0 : loadingPriority(json);
                if (best == null || priority >= bestPriority) {
                    best = new Version(source, json);
                    bestPriority = priority;
                }
            }
            if (best != null) chosen.put(id, best);
        }
        return chosen;
    }

    public static int loadingPriority(JsonElement json) {
        return new Dynamic<>(JsonOps.INSTANCE, json).get("loading_priority").asInt(0);
    }

    public static @Nullable JsonElement read(Source source, ResourceLocation id, Logger log) {
        return read(source, id, log, null);
    }

    public static @Nullable JsonElement read(Source source, ResourceLocation id, Logger log, @Nullable String mustMention) {
        byte[] bytes;
        try (InputStream in = source.resource().open()) {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            log.error("Couldn't read data file {} ({} in {}): {}", id, source.file(), source.packId(), e.getMessage());
            return null;
        }
        if (mustMention != null && !contains(bytes, mustMention.getBytes(StandardCharsets.UTF_8))) return null;
        try {
            JsonTextParser.Parsed parsed = JsonTextParser.read(bytes, source.format());
            if (parsed.trailingLine() > 0) {
                log.warn("Data file {} ({} in {}) has text after its last closing bracket on line {}, which is ignored",
                    id, source.file(), source.packId(), parsed.trailingLine());
            }
            return parsed.value();
        } catch (JsonParseException e) {
            log.error("Couldn't parse data file {} ({} in {}): {}", id, source.file(), source.packId(), e.getMessage());
            return null;
        }
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        int last = haystack.length - needle.length;
        byte first = needle[0];
        for (int i = 0; i <= last; i++) {
            if (haystack[i] != first) continue;
            int k = 1;
            while (k < needle.length && haystack[i + k] == needle[k]) k++;
            if (k == needle.length) return true;
        }
        return false;
    }

    public static boolean isDataFile(ResourceLocation location) {
        return JsonFormat.of(location.getPath()) != null;
    }

    public static @Nullable ResourceLocation idOf(ResourceLocation file, String directory, @Nullable JsonFormat format) {
        if (format == null) return null;
        String path = file.getPath();
        int start = directory.length() + 1;
        int stop = path.length() - format.extension().length();
        if (stop <= start || !path.startsWith(directory) || path.charAt(directory.length()) != '/') return null;
        return file.withPath(path.substring(start, stop));
    }

    private static final class PackOrder {
        private final List<PackResources> packs;
        private final IdentityHashMap<PackResources, Integer> index;

        private PackOrder(ResourceManager manager) {
            this.packs = manager.listPacks().toList();
            this.index = new IdentityHashMap<>(packs.size());
            for (int i = 0; i < packs.size(); i++) index.put(packs.get(i), i);
        }

        private int topRank(Source source) {
            Integer known = index.get(source.resource().source());
            if (known != null) return known;
            for (int i = packs.size() - 1; i >= 0; i--) {
                if (packs.get(i).getResource(PackType.SERVER_DATA, source.file()) != null) return i;
            }
            return -1;
        }

        private int compare(Source a, Source b, ResourceLocation id, Logger log) {
            int byPack = Integer.compare(topRank(a), topRank(b));
            if (byPack != 0) return byPack;
            Source kept = a.format().ordinal() < b.format().ordinal() ? a : b;
            log.warn("Data file {} is defined twice in {} ({} and {}); using {}",
                id, a.packId(), a.file(), b.file(), kept.file());
            return Integer.compare(b.format().ordinal(), a.format().ordinal());
        }

        private List<Source> merge(List<Source> first, List<Source> second, ResourceLocation id, Logger log) {
            List<Source> merged = new ArrayList<>(first.size() + second.size());
            int[] firstRanks = ranks(first);
            int[] secondRanks = ranks(second);
            int i = 0;
            int j = 0;
            while (i < first.size() || j < second.size()) {
                if (j >= second.size()) {
                    merged.add(first.get(i++));
                } else if (i >= first.size()) {
                    merged.add(second.get(j++));
                } else if (firstRanks[i] != secondRanks[j]) {
                    merged.add(firstRanks[i] < secondRanks[j] ? first.get(i++) : second.get(j++));
                } else {
                    Source a = first.get(i);
                    Source b = second.get(j);
                    compare(a, b, id, log);
                    if (a.format().ordinal() > b.format().ordinal()) {
                        merged.add(first.get(i++));
                    } else {
                        merged.add(second.get(j++));
                    }
                }
            }
            return merged;
        }

        private int[] ranks(List<Source> sources) {
            int[] ranks = new int[sources.size()];
            int floor = 0;
            boolean previousKnown = false;
            for (int k = 0; k < ranks.length; k++) {
                Source source = sources.get(k);
                Integer known = index.get(source.resource().source());
                int rank;
                if (known != null) {
                    rank = Math.max(known, floor);
                } else {
                    rank = lowestPackWith(source.file(), previousKnown ? floor + 1 : floor);
                }
                ranks[k] = rank;
                floor = rank;
                previousKnown = known != null;
            }
            return ranks;
        }

        private int lowestPackWith(ResourceLocation file, int from) {
            for (int i = from; i < packs.size(); i++) {
                if (packs.get(i).getResource(PackType.SERVER_DATA, file) != null) return i;
            }
            return packs.size();
        }
    }
}
