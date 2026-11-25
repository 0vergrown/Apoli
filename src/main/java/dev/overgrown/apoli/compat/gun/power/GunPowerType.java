package dev.overgrown.apoli.compat.gun.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerType;

public final class GunPowerType<C> extends PowerType<C> {
    private final MapCodec<C> codec;

    public GunPowerType(MapCodec<C> codec) {
        this.codec = codec;
    }

    @Override
    public MapCodec<C> configCodec() {
        return codec;
    }
}
