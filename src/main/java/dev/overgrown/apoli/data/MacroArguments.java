package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public record MacroArguments(Optional<ResourceLocation> storage, String path, Optional<CompoundTag> values,
                             Map<String, ResourceLocation> resources,
                             Map<String, ResourceLocation> targetResources) {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    public MacroArguments {
        resources = Map.copyOf(resources);
        targetResources = Map.copyOf(targetResources);
    }

    /** Either {"key": "ns:power"} or ["ns:power"], where the list form keys each entry by its path. */
    private static final Codec<Map<String, ResourceLocation>> RESOURCE_MAP = Codec.either(
        Codec.unboundedMap(Codec.STRING, IdCodecs.ID),
        IdCodecs.ID.listOf()
    ).xmap(
        either -> either.map(java.util.function.Function.identity(), MacroArguments::keyByPath),
        Either::left);

    public static final Codec<MacroArguments> CODEC = RecordCodecBuilder.create(i -> i.group(
        IdCodecs.ID.optionalFieldOf("storage").forGetter(MacroArguments::storage),
        Codec.STRING.optionalFieldOf("path", "").forGetter(MacroArguments::path),
        CompoundTag.CODEC.optionalFieldOf("values").forGetter(MacroArguments::values),
        RESOURCE_MAP.optionalFieldOf("resources", Map.of()).forGetter(MacroArguments::resources),
        RESOURCE_MAP.optionalFieldOf("target_resources", Map.of()).forGetter(MacroArguments::targetResources)
    ).apply(i, MacroArguments::new));

    public static final MapCodec<Optional<MacroArguments>> FIELD =
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("arguments", CODEC);

    private static Map<String, ResourceLocation> keyByPath(java.util.List<ResourceLocation> ids) {
        Map<String, ResourceLocation> out = new LinkedHashMap<>(ids.size());
        for (ResourceLocation id : ids) {
            String path = id.getPath();
            out.put(path.substring(path.lastIndexOf('/') + 1), id);
        }
        return out;
    }

    public CompoundTag resolve(MinecraftServer server) {
        return resolve(server, null, null);
    }

    public CompoundTag resolve(MinecraftServer server, @Nullable Entity self, @Nullable Entity target) {
        CompoundTag out = new CompoundTag();
        if (storage.isPresent()) {
            CompoundTag stored = StorageTarget.read(server, storage.get(), path);
            if (stored != null) out.merge(stored);
        }
        readResources(out, resources, self);
        readResources(out, targetResources, target);
        values.ifPresent(out::merge);
        return out;
    }

    private static void readResources(CompoundTag out, Map<String, ResourceLocation> wanted, @Nullable Entity holder) {
        if (wanted.isEmpty()) return;
        PowerContainer container = holder == null ? null : PowerContainer.of(holder);
        if (container == null) return;
        for (Map.Entry<String, ResourceLocation> entry : wanted.entrySet()) {
            ResourceLocation powerId = entry.getValue();
            OptionalInt value = PowerResources.read(container, powerId);
            if (value.isEmpty()) continue;
            String key = entry.getKey();
            out.putInt(key, value.getAsInt());
            PowerResources.bound(container, powerId, true).ifPresent(max -> out.putInt(key + "_max", max));
            PowerResources.bound(container, powerId, false).ifPresent(min -> out.putInt(key + "_min", min));
        }
    }

    public static String expand(String command, CompoundTag arguments) {
        int open = command.indexOf("$(");
        if (open < 0) return command;
        StringBuilder out = new StringBuilder(command.length());
        int cursor = 0;
        while (open >= 0) {
            int close = command.indexOf(')', open + 2);
            if (close < 0) break;
            String key = command.substring(open + 2, close);
            Tag value = arguments.get(key);
            if (value == null) {
                warnMissing(command, key);
                return null;
            }
            out.append(command, cursor, open).append(render(value));
            cursor = close + 1;
            open = command.indexOf("$(", cursor);
        }
        return out.append(command, cursor, command.length()).toString();
    }

    private static void warnMissing(String command, String key) {
        if (!WARNED.add(key + '\0' + command)) return;
        LOG.warn("[Apoli] apoli:execute_command skipped \"{}\" — nothing supplied a value for $({}). "
            + "Check the 'arguments' object: a resource the holder does not have, or a storage path that is empty, "
            + "leaves its key undefined.", command, key);
    }

    private static String render(Tag tag) {
        if (tag instanceof FloatTag value) return String.valueOf(value.getAsFloat());
        if (tag instanceof DoubleTag value) return String.valueOf(value.getAsDouble());
        if (tag instanceof ByteTag value) return String.valueOf(value.getAsByte());
        if (tag instanceof ShortTag value) return String.valueOf(value.getAsShort());
        if (tag instanceof LongTag value) return String.valueOf(value.getAsLong());
        return tag.getAsString();
    }
}
