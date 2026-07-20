package dev.overgrown.apoli.compat.hardcorerevival.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.blay09.mods.hardcorerevival.api.HardcoreRevivalAPI;
import net.minecraft.world.entity.player.Player;

public final class ReviveAction implements ActionType<EntityCtx, ReviveAction.Cfg> {
    public record Cfg(boolean applyEffects) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("apply_effects", true).forGetter(Cfg::applyEffects)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (ctx.raw() instanceof Player player && !player.level().isClientSide()) {
            HardcoreRevivalAPI.wakeup(player, cfg.applyEffects());
        }
    }
}
