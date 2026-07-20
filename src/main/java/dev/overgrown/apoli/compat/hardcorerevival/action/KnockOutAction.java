package dev.overgrown.apoli.compat.hardcorerevival.action;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.blay09.mods.hardcorerevival.api.HardcoreRevivalAPI;
import net.minecraft.world.entity.player.Player;

public final class KnockOutAction implements ActionType<EntityCtx, KnockOutAction.Cfg> {
    public record Cfg() {}

    @Override
    public MapCodec<Cfg> codec() {
        return MapCodec.unit(new Cfg());
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (ctx.raw() instanceof Player player && !player.level().isClientSide()) {
            HardcoreRevivalAPI.knockout(player, player.damageSources().generic());
        }
    }
}
