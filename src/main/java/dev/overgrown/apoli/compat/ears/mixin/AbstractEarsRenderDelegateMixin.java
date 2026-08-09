package dev.overgrown.apoli.compat.ears.mixin;

import com.unascribed.ears.common.render.AbstractEarsRenderDelegate;
import com.unascribed.ears.common.render.EarsRenderDelegate;
import dev.overgrown.apoli.client.render.SkinRenderCompat;
import dev.overgrown.apoli.power.builtin.ModelColorPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = AbstractEarsRenderDelegate.class, remap = false)
@OnlyIn(Dist.CLIENT)
public abstract class AbstractEarsRenderDelegateMixin {

    private static final String ADD_VERTEX =
        "Lcom/unascribed/ears/common/render/AbstractEarsRenderDelegate;addVertex(FFIFFFFFFFFF)V";
    private static final String RENDER_FRONT =
        "renderFront(IIIILcom/unascribed/ears/common/render/EarsRenderDelegate$TexRotation;"
            + "Lcom/unascribed/ears/common/render/EarsRenderDelegate$TexFlip;"
            + "Lcom/unascribed/ears/common/render/EarsRenderDelegate$QuadGrow;)V";
    private static final String RENDER_BACK =
        "renderBack(IIIILcom/unascribed/ears/common/render/EarsRenderDelegate$TexRotation;"
            + "Lcom/unascribed/ears/common/render/EarsRenderDelegate$TexFlip;"
            + "Lcom/unascribed/ears/common/render/EarsRenderDelegate$QuadGrow;)V";

    @Shadow
    protected Object peer;

    @Unique
    private float[] apoli$colour = ModelColorPower.IDENTITY;

    @Inject(method = RENDER_FRONT, at = @At("HEAD"))
    private void apoli$sampleFrontColour(int u, int v, int w, int h, EarsRenderDelegate.TexRotation rotation,
                                         EarsRenderDelegate.TexFlip flip, EarsRenderDelegate.QuadGrow grow,
                                         CallbackInfo ci) {
        apoli$sampleColour();
    }

    @Inject(method = RENDER_BACK, at = @At("HEAD"))
    private void apoli$sampleBackColour(int u, int v, int w, int h, EarsRenderDelegate.TexRotation rotation,
                                        EarsRenderDelegate.TexFlip flip, EarsRenderDelegate.QuadGrow grow,
                                        CallbackInfo ci) {
        apoli$sampleColour();
    }

    @ModifyArgs(method = {RENDER_FRONT, RENDER_BACK}, at = @At(value = "INVOKE", target = ADD_VERTEX))
    private void apoli$tintVertex(Args args) {
        float[] colour = apoli$colour;
        if (colour == ModelColorPower.IDENTITY) return;
        args.set(3, args.<Float>get(3) * colour[0]);
        args.set(4, args.<Float>get(4) * colour[1]);
        args.set(5, args.<Float>get(5) * colour[2]);
        args.set(6, args.<Float>get(6) * colour[3]);
    }

    @Unique
    private void apoli$sampleColour() {
        apoli$colour = this.peer instanceof LivingEntity entity
            ? SkinRenderCompat.rgba(entity)
            : ModelColorPower.IDENTITY;
    }
}
