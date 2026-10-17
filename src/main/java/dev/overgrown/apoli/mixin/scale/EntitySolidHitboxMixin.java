package dev.overgrown.apoli.mixin.scale;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.SolidHitboxPower;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntitySolidHitboxMixin {

    @ModifyReturnValue(method = "canBeCollidedWith", at = @At("RETURN"))
    private boolean apoli$solidHitbox(boolean original) {
        return original || SolidHitboxPower.isSolid((Entity) (Object) this);
    }

    @ModifyReturnValue(method = "isPushable", at = @At("RETURN"))
    private boolean apoli$solidNotPushable(boolean original) {
        return original && SolidHitboxPower.isPushable((Entity) (Object) this);
    }

    @ModifyReturnValue(method = "canCollideWith", at = @At("RETURN"))
    private boolean apoli$solidOnlyForSome(boolean original, Entity other) {
        if (!original || !SolidHitboxPower.isSolid(other)) return original;
        return SolidHitboxPower.isSolidFor(other, (Entity) (Object) this);
    }
}
