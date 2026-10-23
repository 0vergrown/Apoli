package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;

public final class BreedAction implements ActionType<BiEntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof Animal parent) || !(ctx.target() instanceof Animal mate) || parent == mate) return;
        if (!(parent.level() instanceof ServerLevel level) || parent.isBaby() || mate.isBaby()) return;
        if (parent.getClass() != mate.getClass() && !(horseOrDonkey(parent) && horseOrDonkey(mate))) return;
        parent.spawnChildFromBreeding(level, mate);
    }

    private static boolean horseOrDonkey(Animal animal) {
        return animal instanceof Horse || animal instanceof Donkey;
    }
}
