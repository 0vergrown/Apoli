package dev.overgrown.apoli.mixin.flag;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PhasingPower;
import dev.overgrown.apoli.power.ApoliIds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class FogRendererPhasingMixin {

    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    @Inject(method = "setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V",
        at = @At("RETURN"))
    private static void apoli$phasingBlindnessColor(Camera camera, float partialTick, ClientLevel level,
                                                     int renderDistance, float darkness, CallbackInfo ci) {
        Entity focus = camera.getEntity();
        if (!(focus instanceof LivingEntity living)) return;
        if (!hasBlindnessPhasing(living)) return;
        if (!eyeInSolidBlock(living)) return;
        fogRed = 0.0F;
        fogGreen = 0.0F;
        fogBlue = 0.0F;
    }

    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V",
        at = @At("RETURN"))
    private static void apoli$phasingBlindnessFog(Camera camera, FogRenderer.FogMode fogMode, float f, boolean bl,
                                                   float g, CallbackInfo ci) {
        Entity focus = camera.getEntity();
        if (!(focus instanceof LivingEntity living)) return;
        float view = blindnessViewDistance(living);
        if (view < 0.0F) return;
        if (!eyeInSolidBlock(living)) return;
        if (fogMode == FogRenderer.FogMode.FOG_SKY) {
            RenderSystem.setShaderFogStart(0.0F);
            RenderSystem.setShaderFogEnd(view * 0.8F);
        } else {
            RenderSystem.setShaderFogStart(view * 0.25F);
            RenderSystem.setShaderFogEnd(view);
        }
    }

    private static boolean hasBlindnessPhasing(LivingEntity entity) {
        return blindnessViewDistance(entity) >= 0.0F;
    }

    private static float blindnessViewDistance(LivingEntity entity) {
        float[] best = new float[]{-1.0F};
        PowerLookup.forEach(entity, ApoliIds.PHASING, PhasingPower.Config.class, cfg -> {
            if (cfg.renderType() != PhasingPower.RenderType.BLINDNESS) return;
            float v = cfg.viewDistance();
            if (best[0] < 0.0F || v < best[0]) best[0] = v;
        });
        return best[0];
    }

    private static boolean eyeInSolidBlock(LivingEntity entity) {
        BlockPos eyePos = BlockPos.containing(entity.getEyePosition());
        BlockState state = entity.level().getBlockState(eyePos);
        if (state.isAir()) return false;
        return !state.getCollisionShape(entity.level(), eyePos).isEmpty();
    }
}
