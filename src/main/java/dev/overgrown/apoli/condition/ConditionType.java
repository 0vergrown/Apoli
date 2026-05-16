package dev.overgrown.apoli.condition;

import com.mojang.serialization.MapCodec;

public interface ConditionType<CTX, C> {
    MapCodec<C> codec();
    boolean test(C config, CTX ctx);
}