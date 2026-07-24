package dev.overgrown.apoli.action;

import com.mojang.serialization.MapCodec;

public interface ActionType<CTX, C> {
    MapCodec<C> codec();
    void run(C config, CTX ctx);
}
