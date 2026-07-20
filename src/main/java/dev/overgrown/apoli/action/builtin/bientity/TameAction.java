package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;

public final class TameAction implements ActionType<BiEntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, BiEntityCtx ctx) {
        if (!(ctx.target() instanceof TamableAnimal tame)) return;
        if (!(ctx.actor() instanceof Player player)) return;
        tame.setOwnerUUID(player.getUUID());
        tame.setTame(true);
        if (tame instanceof Wolf wolf) wolf.setOrderedToSit(true);
    }
}
