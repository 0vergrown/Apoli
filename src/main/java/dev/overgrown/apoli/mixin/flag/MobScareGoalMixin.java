package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.entity.ScareMobsGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobScareGoalMixin {

    @Shadow
    protected GoalSelector goalSelector;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",
        at = @At("TAIL"))
    private void apoli$addScareGoal(EntityType<? extends Mob> type, Level level, CallbackInfo ci) {
        if (level.isClientSide()) return;
        if (!((Object) this instanceof PathfinderMob pathfinder)) return;
        this.goalSelector.addGoal(0, new ScareMobsGoal(pathfinder));
    }
}
