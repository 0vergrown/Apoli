package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;

public final class AttackTargetCondition implements ConditionType<BiEntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, BiEntityCtx ctx) {
        LivingEntity actor = ctx.actor();
        LivingEntity target = ctx.target();
        if (actor instanceof Mob mob && target.equals(mob.getTarget())) return true;
        if (actor instanceof NeutralMob neutral && target.equals(neutral.getTarget())) return true;
        return false;
    }
}
