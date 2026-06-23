package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatType;
import org.jetbrains.annotations.Nullable;

public record Stat(ResourceLocation type, ResourceLocation id) {
    public static final Codec<Stat> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceLocation.CODEC.fieldOf("type").forGetter(Stat::type),
        ResourceLocation.CODEC.fieldOf("id").forGetter(Stat::id)
    ).apply(i, Stat::new));

    @SuppressWarnings({"rawtypes", "unchecked"})
    public @Nullable net.minecraft.stats.Stat<?> resolve() {
        StatType statType = BuiltInRegistries.STAT_TYPE.get(type);
        if (statType == null) return null;
        Registry<?> reg = statType.getRegistry();
        Object value = reg.get(id);
        if (value == null) return null;
        return statType.get(value);
    }
}
