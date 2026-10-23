package dev.overgrown.apoli.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class IngredientTypes {
    private static final Map<ResourceLocation, Ingredient.CustomType<?>> FABRIC = new HashMap<>();

    private static final Codec<CompoundTag> NBT = Codec.either(
        Codec.STRING.comapFlatMap(IngredientTypes::parseSnbt, CompoundTag::toString),
        CompoundTag.CODEC
    ).xmap(either -> either.map(tag -> tag, tag -> tag), Either::left);

    static {
        register(FABRIC, new Ingredient.CustomType<NbtEntry>(Ingredient.FABRIC_KEY, Ingredient.id("fabric", "nbt"),
            type -> RecordCodecBuilder.<NbtEntry>mapCodec(i -> i.group(
                Ingredient.SELF.fieldOf("base").forGetter(NbtEntry::base),
                NBT.optionalFieldOf("nbt").forGetter(NbtEntry::nbt),
                Codec.BOOL.optionalFieldOf("strict", false).forGetter(NbtEntry::strict)
            ).apply(i, (base, nbt, strict) -> new NbtEntry(type, base, nbt, strict)))
                .flatXmap(IngredientTypes::requireNbt, DataResult::success)));
    }

    private IngredientTypes() {}

    static @Nullable Ingredient.CustomType<?> find(boolean fabric, ResourceLocation id) {
        return fabric ? FABRIC.get(id) : null;
    }

    private static void register(Map<ResourceLocation, Ingredient.CustomType<?>> types, Ingredient.CustomType<?> type) {
        types.put(type.id(), type);
    }

    private static DataResult<CompoundTag> parseSnbt(String snbt) {
        try {
            return DataResult.success(TagParser.parseTag(snbt));
        } catch (CommandSyntaxException e) {
            return DataResult.error(() -> "Invalid SNBT in \"nbt\": " + e.getMessage());
        }
    }

    private static DataResult<NbtEntry> requireNbt(NbtEntry entry) {
        if (entry.nbt().isPresent() || entry.strict()) return DataResult.success(entry);
        return DataResult.error(() -> "fabric:nbt needs \"nbt\" unless \"strict\" is true");
    }

    public record NbtEntry(Ingredient.CustomType<NbtEntry> type, Ingredient base, Optional<CompoundTag> nbt, boolean strict)
        implements Ingredient.Custom {
        @Override
        public boolean test(ItemStack stack) {
            if (!base.test(stack)) return false;
            CompoundTag tag = stack.getTag();
            if (strict) return nbt.isEmpty() ? tag == null : NbtComparison.sameValue(nbt.get(), tag);
            return NbtComparison.matches(nbt.get(), tag);
        }
    }
}
