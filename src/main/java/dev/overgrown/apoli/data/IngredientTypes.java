package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.SingleOrList;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class IngredientTypes {
    private static final Map<ResourceLocation, Ingredient.CustomType<?>> FABRIC = new HashMap<>();
    private static final Map<ResourceLocation, Ingredient.CustomType<?>> NEOFORGE = new HashMap<>();

    static {
        register(FABRIC, new Ingredient.CustomType<ComponentsEntry>(Ingredient.FABRIC_KEY, Ingredient.id("fabric", "components"),
            type -> RecordCodecBuilder.<ComponentsEntry>mapCodec(i -> i.group(
                Ingredient.SELF.fieldOf("base").forGetter(ComponentsEntry::base),
                DataComponentPatch.CODEC.fieldOf("components").forGetter(ComponentsEntry::components)
            ).apply(i, (base, components) -> new ComponentsEntry(type, base, components)))
                .flatXmap(IngredientTypes::requireComponents, DataResult::success)));
        register(FABRIC, new Ingredient.CustomType<CustomDataEntry>(Ingredient.FABRIC_KEY, Ingredient.id("fabric", "custom_data"),
            type -> RecordCodecBuilder.<CustomDataEntry>mapCodec(i -> i.group(
                Ingredient.SELF.fieldOf("base").forGetter(CustomDataEntry::base),
                TagParser.LENIENT_CODEC.fieldOf("nbt").forGetter(CustomDataEntry::nbt)
            ).apply(i, (base, nbt) -> new CustomDataEntry(type, base, nbt)))
                .flatXmap(IngredientTypes::requireNbt, DataResult::success)));
        register(NEOFORGE, new Ingredient.CustomType<PredicateEntry>(Ingredient.TYPE_KEY, Ingredient.id("neoforge", "components"),
            type -> RecordCodecBuilder.<PredicateEntry>mapCodec(i -> i.group(
                SingleOrList.of(IdOrTag.<Item>codec(Registries.ITEM)).fieldOf("items").forGetter(PredicateEntry::items),
                DataComponentPredicate.CODEC.fieldOf("components").forGetter(PredicateEntry::components),
                Codec.BOOL.optionalFieldOf("strict", false).forGetter(PredicateEntry::strict)
            ).apply(i, (items, components, strict) -> new PredicateEntry(type, items, components, strict)))));
    }

    private IngredientTypes() {}

    static @Nullable Ingredient.CustomType<?> find(boolean fabric, ResourceLocation id) {
        return (fabric ? FABRIC : NEOFORGE).get(id);
    }

    private static void register(Map<ResourceLocation, Ingredient.CustomType<?>> types, Ingredient.CustomType<?> type) {
        types.put(type.id(), type);
    }

    private static DataResult<ComponentsEntry> requireComponents(ComponentsEntry entry) {
        if (!entry.components().isEmpty()) return DataResult.success(entry);
        return DataResult.error(() -> "fabric:components needs at least one entry in \"components\"");
    }

    private static DataResult<CustomDataEntry> requireNbt(CustomDataEntry entry) {
        if (!entry.nbt().isEmpty()) return DataResult.success(entry);
        return DataResult.error(() -> "fabric:custom_data needs a non-empty \"nbt\"; use fabric:components for an exact match");
    }

    static boolean sameValue(Object expected, @Nullable Object actual) {
        if (actual == null) return false;
        if (expected instanceof CustomData wanted && actual instanceof CustomData held) {
            return NbtComparison.sameValue(wanted.getUnsafe(), held.getUnsafe());
        }
        return expected.equals(actual);
    }

    public static final class ComponentsEntry implements Ingredient.Custom {
        private final Ingredient.CustomType<ComponentsEntry> type;
        private final Ingredient base;
        private final DataComponentPatch components;
        private final DataComponentType<?>[] types;
        private final Object[] expected;

        ComponentsEntry(Ingredient.CustomType<ComponentsEntry> type, Ingredient base, DataComponentPatch components) {
            this.type = type;
            this.base = base;
            this.components = components;
            this.types = new DataComponentType<?>[components.size()];
            this.expected = new Object[components.size()];
            int i = 0;
            for (Map.Entry<DataComponentType<?>, Optional<?>> entry : components.entrySet()) {
                this.types[i] = entry.getKey();
                this.expected[i] = entry.getValue().orElse(null);
                i++;
            }
        }

        @Override
        public Ingredient.CustomType<ComponentsEntry> type() {
            return type;
        }

        public Ingredient base() {
            return base;
        }

        public DataComponentPatch components() {
            return components;
        }

        @Override
        public boolean test(ItemStack stack) {
            if (!base.test(stack)) return false;
            for (int i = 0; i < types.length; i++) {
                Object wanted = expected[i];
                if (wanted == null) {
                    if (stack.has(types[i])) return false;
                } else if (!sameValue(wanted, stack.get(types[i]))) {
                    return false;
                }
            }
            return true;
        }
    }

    public record CustomDataEntry(Ingredient.CustomType<CustomDataEntry> type, Ingredient base, CompoundTag nbt)
        implements Ingredient.Custom {
        @Override
        public boolean test(ItemStack stack) {
            if (!base.test(stack)) return false;
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            return data != null && NbtComparison.matches(nbt, data.getUnsafe());
        }
    }

    public static final class PredicateEntry implements Ingredient.Custom {
        private final Ingredient.CustomType<PredicateEntry> type;
        private final List<IdOrTag<Item>> items;
        private final DataComponentPredicate components;
        private final boolean strict;
        private final DataComponentType<?>[] types;
        private final Object[] values;

        PredicateEntry(Ingredient.CustomType<PredicateEntry> type, List<IdOrTag<Item>> items,
                       DataComponentPredicate components, boolean strict) {
            this.type = type;
            this.items = List.copyOf(items);
            this.components = components;
            this.strict = strict;
            DataComponentPatch patch = components.asPatch();
            this.types = new DataComponentType<?>[patch.size()];
            this.values = new Object[patch.size()];
            int i = 0;
            for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
                this.types[i] = entry.getKey();
                this.values[i] = entry.getValue().orElse(null);
                i++;
            }
        }

        @Override
        public Ingredient.CustomType<PredicateEntry> type() {
            return type;
        }

        public List<IdOrTag<Item>> items() {
            return items;
        }

        public DataComponentPredicate components() {
            return components;
        }

        public boolean strict() {
            return strict;
        }

        @Override
        public boolean test(ItemStack stack) {
            Holder<Item> item = stack.getItemHolder();
            boolean listed = false;
            for (int i = 0, n = items.size(); i < n; i++) {
                if (items.get(i).matches(item)) {
                    listed = true;
                    break;
                }
            }
            if (!listed) return false;
            for (int i = 0; i < types.length; i++) {
                if (values[i] == null || !sameValue(values[i], stack.get(types[i]))) return false;
            }
            if (strict) {
                for (Map.Entry<DataComponentType<?>, Optional<?>> entry : stack.getComponentsPatch().entrySet()) {
                    if (!expects(entry.getKey())) return false;
                }
            }
            return true;
        }

        private boolean expects(DataComponentType<?> componentType) {
            for (DataComponentType<?> candidate : types) {
                if (candidate == componentType) return true;
            }
            return false;
        }
    }
}
