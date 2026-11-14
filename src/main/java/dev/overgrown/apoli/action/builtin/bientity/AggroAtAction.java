package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

public final class AggroAtAction implements ActionType<BiEntityCtx, AggroAtAction.Cfg> {

    public record Cfg(Expression duration) {}

    private static final Expression FOREVER = Expression.constant(-1);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Expression.INT_OR_EXPR.optionalFieldOf("duration", FOREVER).forGetter(Cfg::duration)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        if (!(ctx.target() instanceof Mob mob)) return;
        if (!(ctx.actor() instanceof LivingEntity actor) || actor == mob || !actor.isAlive()) return;

        int duration = cfg.duration.evalInt(actor);
        if (mob instanceof NeutralMob neutralMob) {
            neutralMob.setPersistentAngerTarget(actor.getUUID());
            neutralMob.setRemainingPersistentAngerTime(duration < 0 ? Integer.MAX_VALUE : duration);
            if (actor instanceof Player) return;
        }

        mob.setLastHurtByMob(actor);
        mob.setTarget(actor);
        Brain<?> brain = mob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
            if (duration < 0) brain.setMemory(MemoryModuleType.ATTACK_TARGET, actor);
            else brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, actor, duration);
        }
        if (brain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED)) {
            if (duration < 0) brain.setMemory(MemoryModuleType.ANGRY_AT, actor.getUUID());
            else brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, actor.getUUID(), duration);
        }
    }
}
