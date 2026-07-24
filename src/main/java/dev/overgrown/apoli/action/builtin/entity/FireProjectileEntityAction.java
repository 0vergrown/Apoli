package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

public final class FireProjectileEntityAction implements ActionType<EntityCtx, FireProjectilePower.Config> {

    @Override
    public MapCodec<FireProjectilePower.Config> codec() {
        return AliasingMapCodec.wrap(FireProjectilePower.CONFIG_CODEC, Map.of("entity_id", "entity_type"));
    }

    @Override
    public void run(FireProjectilePower.Config cfg, EntityCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        FireProjectilePower.fireBurst(ctx.entity(), level, cfg);
    }
}
