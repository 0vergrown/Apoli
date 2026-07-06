package dev.overgrown.apoli.compat.hardcorerevival.condition;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.blay09.mods.hardcorerevival.HardcoreRevival;
import net.minecraft.world.entity.player.Player;


public final class KnockedOutCondition implements ConditionType<EntityCtx, KnockedOutCondition.Cfg> {
    public record Cfg() {}

    @Override
    public MapCodec<Cfg> codec() {
        return MapCodec.unit(new Cfg());
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        return ctx.raw() instanceof Player player
            && HardcoreRevival.getManager().getRevivalData(player).isKnockedOut();
    }
}
