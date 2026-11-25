package dev.overgrown.apoli.compat.gun;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record GunFilter(List<ResourceLocation> guns) {

    public static final MapCodec<GunFilter> MAP_CODEC = LoggedOptionalField.strict("gun", SingleOrList.of(IdCodecs.ID))
        .xmap(o -> new GunFilter(o.orElse(List.of())), f -> f.guns().isEmpty() ? Optional.empty() : Optional.of(f.guns()));

    public boolean test(@Nullable ResourceLocation gun) {
        return guns.isEmpty() || (gun != null && guns.contains(gun));
    }
}
