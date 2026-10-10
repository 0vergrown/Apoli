package dev.overgrown.apoli.mixin.scale;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.render.ScaleRender;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class PlayerRendererScaleMixin {

    @ModifyReturnValue(method = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;",
        at = @At("RETURN"))
    private Vec3 apoli$scaleCrouchOffset(Vec3 offset, AbstractClientPlayer player, float partialTick) {
        return ScaleRender.crouchOffset(player, offset, partialTick);
    }
}
