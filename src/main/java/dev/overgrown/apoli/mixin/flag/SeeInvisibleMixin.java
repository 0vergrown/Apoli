package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.SeeInvisiblePower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// apoli:see_invisible. The priority puts this after InvisibilityMixin, so it also reveals apoli:invisibility.
@Mixin(value = Entity.class, priority = 1500)
public abstract class SeeInvisibleMixin {
    @ModifyReturnValue(method = "isInvisibleTo", at = @At("RETURN"))
    private boolean apoli$seeInvisible(boolean original, Player observer) {
        return original && Float.isNaN(SeeInvisiblePower.revealOpacity(observer, (Entity) (Object) this));
    }
}
