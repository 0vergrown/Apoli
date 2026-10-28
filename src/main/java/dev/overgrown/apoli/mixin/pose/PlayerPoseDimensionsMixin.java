package dev.overgrown.apoli.mixin.pose;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.data.EntityPoses;
import dev.overgrown.apoli.power.builtin.PosePower;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerPoseDimensionsMixin {

    @ModifyReturnValue(method = "getDimensions", at = @At("RETURN"))
    private EntityDimensions apoli$forcedPoseDimensions(EntityDimensions original, Pose pose) {
        if (!EntityPoses.resizesPlayer(pose) || !PosePower.hasEntityPose((Player) (Object) this, pose)) return original;
        EntityDimensions forced = EntityPoses.playerDimensions(pose);
        return forced != null ? forced : original;
    }

    @ModifyReturnValue(method = "getStandingEyeHeight", at = @At("RETURN"))
    private float apoli$forcedPoseEyeHeight(float original, Pose pose, EntityDimensions dimensions) {
        if (!EntityPoses.resizesPlayer(pose) || !PosePower.hasEntityPose((Player) (Object) this, pose)) return original;
        float forced = EntityPoses.playerEyeHeight(pose);
        return forced >= 0.0F ? forced : original;
    }
}
