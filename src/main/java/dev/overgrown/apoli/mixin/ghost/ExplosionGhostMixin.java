package dev.overgrown.apoli.mixin.ghost;

import dev.overgrown.apoli.block.GhostBlocks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionGhostMixin {

    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;
    @Shadow @Final private Level level;

    @Inject(method = "explode", at = @At("RETURN"))
    private void apoli$spareGhostBlocks(CallbackInfo ci) {
        if (this.toBlow.isEmpty()) return;
        this.toBlow.removeIf(pos -> GhostBlocks.isGhost(this.level, pos));
    }
}
