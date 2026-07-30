package dev.overgrown.apoli.mixin.input;

import dev.overgrown.apoli.client.CursorSpeedState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
@Environment(EnvType.CLIENT)
public abstract class EntityTurnCursorSpeedMixin {

    @ModifyVariable(method = "turn(DD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double apoli$scaleTurnYaw(double yRot) {
        if (!CursorSpeedState.appliesTo((Entity) (Object) this)) return yRot;
        return yRot * CursorSpeedState.horizontal();
    }

    @ModifyVariable(method = "turn(DD)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double apoli$scaleTurnPitch(double xRot) {
        if (!CursorSpeedState.appliesTo((Entity) (Object) this)) return xRot;
        return xRot * CursorSpeedState.vertical();
    }
}
