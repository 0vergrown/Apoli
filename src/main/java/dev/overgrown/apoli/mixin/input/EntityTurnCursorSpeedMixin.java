package dev.overgrown.apoli.mixin.input;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.overgrown.apoli.client.CursorSpeedState;
import dev.overgrown.apoli.client.MouseMovementWatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
@Environment(EnvType.CLIENT)
public abstract class EntityTurnCursorSpeedMixin {

    @WrapMethod(method = "turn(DD)V")
    private void apoli$turn(double yRot, double xRot, Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        MouseMovementWatcher.onTurn(self, yRot, xRot);
        if (!CursorSpeedState.appliesTo(self)) {
            original.call(yRot, xRot);
            return;
        }
        original.call(yRot * CursorSpeedState.horizontal(), xRot * CursorSpeedState.vertical());
    }
}
