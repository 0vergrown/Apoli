package dev.overgrown.apoli.mixin.possession;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.entity.possession.Possession;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Projectile.class)
public abstract class ProjectilePossessionMixin {

    @ModifyReturnValue(method = "canHitEntity", at = @At("RETURN"))
    private boolean apoli$spareOwnersHost(boolean original, Entity target) {
        if (!original) return false;
        Entity possessor = Possession.possessorOf(target);
        return possessor == null || possessor != ((Projectile) (Object) this).getOwner();
    }
}
