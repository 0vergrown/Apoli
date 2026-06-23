package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.animal.Animal;

public final class SetInLoveAction implements ActionType<BiEntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, BiEntityCtx ctx) {
        if (ctx.target() instanceof Animal animal) {
            animal.setInLove(null);
        }
    }
}
