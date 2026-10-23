package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LazyCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public final class Ingredient {
    public static final String FABRIC_KEY = "fabric:type";
    public static final String NEOFORGE_KEY = "neoforge:ingredient_type";
    public static final String TYPE_KEY = "type";

    private static final String[] DISPATCH_KEYS = {FABRIC_KEY, NEOFORGE_KEY, TYPE_KEY};

    public interface Entry {
        boolean test(ItemStack stack);
    }

    public interface Custom extends Entry {
        CustomType<?> type();
    }

    public static final class CustomType<E extends Custom> {
        private final String key;
        private final ResourceLocation id;
        private final MapCodec<E> codec;

        public CustomType(String key, ResourceLocation id, Function<CustomType<E>, MapCodec<E>> codec) {
            this.key = key;
            this.id = id;
            this.codec = codec.apply(this);
        }

        public String key() {
            return key;
        }

        public ResourceLocation id() {
            return id;
        }

        public MapCodec<E> codec() {
            return codec;
        }

        @SuppressWarnings("unchecked")
        private <T> RecordBuilder<T> encode(Custom entry, DynamicOps<T> ops, RecordBuilder<T> builder) {
            return codec.encode((E) entry, ops, builder.add(key, ops.createString(id.toString())));
        }
    }

    static final Codec<Ingredient> SELF = new LazyCodec<>(() -> Ingredient.CODEC);

    static final Codec<List<Ingredient>> NON_EMPTY_LIST = SELF.listOf().flatXmap(
        list -> list.isEmpty()
            ? DataResult.error(() -> "This custom ingredient needs at least one ingredient in its list")
            : DataResult.success(list),
        DataResult::success);

    private static final Map<ResourceLocation, CustomType<?>> FABRIC_TYPES = new HashMap<>();
    private static final Map<ResourceLocation, CustomType<?>> NEOFORGE_TYPES = new HashMap<>();

    static {
        register(FABRIC_TYPES, anyOf(FABRIC_KEY, "fabric", "any", NON_EMPTY_LIST.fieldOf("ingredients")));
        register(FABRIC_TYPES, allOf(FABRIC_KEY, "fabric", "all", NON_EMPTY_LIST.fieldOf("ingredients")));
        register(FABRIC_TYPES, difference(FABRIC_KEY, "fabric"));
        register(NEOFORGE_TYPES, anyOf(TYPE_KEY, "neoforge", "compound",
            AliasingMapCodec.wrap(NON_EMPTY_LIST.fieldOf("children"), Map.of("ingredients", "children"))));
        register(NEOFORGE_TYPES, allOf(TYPE_KEY, "neoforge", "intersection", NON_EMPTY_LIST.fieldOf("children")));
        register(NEOFORGE_TYPES, difference(TYPE_KEY, "neoforge"));
    }

    public record ItemEntry(IdOrTag<Item> item) implements Entry {
        @Override
        public boolean test(ItemStack stack) {
            return item.matches(stack.getItemHolder());
        }
    }

    public record AnyEntry(CustomType<AnyEntry> type, Ingredient[] children) implements Custom {
        @Override
        public boolean test(ItemStack stack) {
            for (Ingredient child : children) {
                if (child.test(stack)) return true;
            }
            return false;
        }
    }

    public record AllEntry(CustomType<AllEntry> type, Ingredient[] children) implements Custom {
        @Override
        public boolean test(ItemStack stack) {
            for (Ingredient child : children) {
                if (!child.test(stack)) return false;
            }
            return true;
        }
    }

    public record DifferenceEntry(CustomType<DifferenceEntry> type, Ingredient base, Ingredient subtracted) implements Custom {
        @Override
        public boolean test(ItemStack stack) {
            return base.test(stack) && !subtracted.test(stack);
        }
    }

    private static void register(Map<ResourceLocation, CustomType<?>> types, CustomType<?> type) {
        types.put(type.id(), type);
    }

    static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.tryParse(namespace + ":" + path);
    }

    private static CustomType<AnyEntry> anyOf(String key, String namespace, String path, MapCodec<List<Ingredient>> children) {
        return new CustomType<>(key, id(namespace, path), type -> children.xmap(
            list -> new AnyEntry(type, list.toArray(new Ingredient[0])),
            entry -> List.of(entry.children())));
    }

    private static CustomType<AllEntry> allOf(String key, String namespace, String path, MapCodec<List<Ingredient>> children) {
        return new CustomType<>(key, id(namespace, path), type -> children.xmap(
            list -> new AllEntry(type, list.toArray(new Ingredient[0])),
            entry -> List.of(entry.children())));
    }

    private static CustomType<DifferenceEntry> difference(String key, String namespace) {
        return new CustomType<>(key, id(namespace, "difference"),
            type -> RecordCodecBuilder.mapCodec(i -> i.group(
                SELF.fieldOf("base").forGetter(DifferenceEntry::base),
                SELF.fieldOf("subtracted").forGetter(DifferenceEntry::subtracted)
            ).apply(i, (base, subtracted) -> new DifferenceEntry(type, base, subtracted))));
    }

    private record Fields(Optional<ResourceLocation> item, Optional<ResourceLocation> tag) {}

    private static final MapCodec<Fields> FIELDS = RecordCodecBuilder.mapCodec(i -> i.group(
        IdCodecs.ID.optionalFieldOf("item").forGetter(Fields::item),
        IdCodecs.TAG.optionalFieldOf("tag").forGetter(Fields::tag)
    ).apply(i, Fields::new));

    private static final MapCodec<Entry> OBJECT_ENTRY = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of("item", "tag", FABRIC_KEY, NEOFORGE_KEY, TYPE_KEY).map(ops::createString);
        }

        @Override
        public <T> DataResult<Entry> decode(DynamicOps<T> ops, MapLike<T> input) {
            for (String key : DISPATCH_KEYS) {
                T type = input.get(key);
                if (type == null) continue;
                if (TYPE_KEY.equals(key) && hasItemOrTag(input) && !knownType(ops, type)) break;
                return decodeCustom(ops, input, key, type);
            }
            return FIELDS.decode(ops, input).flatMap(Ingredient::fromFields);
        }

        @Override
        public <T> RecordBuilder<T> encode(Entry input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            if (input instanceof Custom custom) return custom.type().encode(custom, ops, prefix);
            IdOrTag<Item> item = ((ItemEntry) input).item();
            return FIELDS.encode(item.isTag()
                ? new Fields(Optional.empty(), Optional.of(item.id()))
                : new Fields(Optional.of(item.id()), Optional.empty()), ops, prefix);
        }
    };

    private static final Codec<IdOrTag<Item>> ITEM_STRING = IdOrTag.codec(Registries.ITEM);

    public static final Codec<Entry> ENTRY_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Entry, T>> decode(DynamicOps<T> ops, T input) {
            if (ops.getStringValue(input).result().isPresent()) {
                return ITEM_STRING.decode(ops, input).map(pair -> pair.mapFirst(ItemEntry::new));
            }
            return OBJECT_ENTRY.codec().decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(Entry input, DynamicOps<T> ops, T prefix) {
            if (input instanceof ItemEntry item) return ITEM_STRING.encode(item.item(), ops, prefix);
            return OBJECT_ENTRY.codec().encode(input, ops, prefix);
        }
    };

    private static final Codec<List<Entry>> ENTRY_LIST = ENTRY_CODEC.listOf();

    public static final Codec<Ingredient> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Ingredient, T>> decode(DynamicOps<T> ops, T input) {
            if (ops.getStream(input).result().isPresent()) {
                return ENTRY_LIST.decode(ops, input).map(pair -> pair.mapFirst(Ingredient::new));
            }
            return ENTRY_CODEC.decode(ops, input).map(pair -> pair.mapFirst(entry -> new Ingredient(List.of(entry))));
        }

        @Override
        public <T> DataResult<T> encode(Ingredient input, DynamicOps<T> ops, T prefix) {
            if (input.entries.length == 1) return ENTRY_CODEC.encode(input.entries[0], ops, prefix);
            return ENTRY_LIST.encode(List.of(input.entries), ops, prefix);
        }
    };

    private final Entry[] entries;

    private Ingredient(List<Entry> entries) {
        this.entries = entries.toArray(new Entry[0]);
    }

    private static <T> boolean hasItemOrTag(MapLike<T> input) {
        return input.get("item") != null || input.get("tag") != null;
    }

    private static <T> boolean knownType(DynamicOps<T> ops, T typeValue) {
        return ops.getStringValue(typeValue).flatMap(IdCodecs::readId).result()
            .map(id -> findType(TYPE_KEY, id) != null)
            .orElse(false);
    }

    private static <T> DataResult<Entry> decodeCustom(DynamicOps<T> ops, MapLike<T> input, String key, T typeValue) {
        return ops.getStringValue(typeValue).flatMap(IdCodecs::readId).flatMap(id -> {
            CustomType<?> type = findType(key, id);
            if (type == null) {
                return DataResult.error(() -> "Unknown custom ingredient type '" + id + "' (given as \"" + key + "\")");
            }
            return type.codec().decode(ops, input).map(entry -> (Entry) entry);
        });
    }

    private static @Nullable CustomType<?> findType(String key, ResourceLocation id) {
        boolean fabric = FABRIC_KEY.equals(key);
        CustomType<?> type = fabric ? FABRIC_TYPES.get(id) : NEOFORGE_TYPES.get(id);
        if (type == null) type = IngredientTypes.find(fabric, id);
        if (type == null) type = LoaderIngredients.find(fabric, id);
        return type;
    }

    private static DataResult<Entry> fromFields(Fields fields) {
        if (fields.item().isPresent()) return DataResult.success(new ItemEntry(IdOrTag.id(fields.item().get())));
        if (fields.tag().isPresent()) return DataResult.success(new ItemEntry(IdOrTag.tag(Registries.ITEM, fields.tag().get())));
        return DataResult.error(() -> "An ingredient needs \"item\", \"tag\", or a custom ingredient type "
            + "(\"" + FABRIC_KEY + "\", \"" + TYPE_KEY + "\" or \"" + NEOFORGE_KEY + "\")");
    }

    public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (Entry entry : entries) {
            if (entry.test(stack)) return true;
        }
        return false;
    }
}
