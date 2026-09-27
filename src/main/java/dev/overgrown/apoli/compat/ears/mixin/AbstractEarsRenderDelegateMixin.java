package dev.overgrown.apoli.compat.ears.mixin;

import com.unascribed.ears.api.EarsFeatureType;
import com.unascribed.ears.api.features.EarsFeatures;
import com.unascribed.ears.common.render.AbstractEarsRenderDelegate;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.client.render.AttachmentParts;
import dev.overgrown.apoli.client.render.SkinRenderCompat;
import dev.overgrown.apoli.compat.ears.EarsAttachments;
import dev.overgrown.apoli.compat.ears.EarsDelegateState;
import dev.overgrown.apoli.data.BodyAttachments;
import dev.overgrown.apoli.power.builtin.ModelColorPower;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractEarsRenderDelegate.class, remap = false)
@OnlyIn(Dist.CLIENT)
public abstract class AbstractEarsRenderDelegateMixin implements EarsDelegateState {

    private static final String ADD_VERTEX = "Lcom/unascribed/ears/common/render/AbstractEarsRenderDelegate;addVertex";
    private static final String QUAD_ARGS = "Lcom/unascribed/ears/common/render/EarsRenderDelegate$TexRotation;"
        + "Lcom/unascribed/ears/common/render/EarsRenderDelegate$TexFlip;"
        + "Lcom/unascribed/ears/common/render/EarsRenderDelegate$QuadGrow;)V";
    private static final String RENDER_FRONT = "renderFront(IIII" + QUAD_ARGS;
    private static final String RENDER_BACK = "renderBack(IIII" + QUAD_ARGS;
    private static final String RENDER_FRONT_SKEW = "renderFrontSkew(IIIIFFF" + QUAD_ARGS;
    private static final String RENDER_BACK_SKEW = "renderBackSkew(IIIIFFF" + QUAD_ARGS;

    @Shadow
    protected Object peer;

    @Unique
    private float[] apoli$colour = ModelColorPower.IDENTITY;
    @Unique
    private float[] apoli$whole = ModelColorPower.IDENTITY;
    @Unique
    private final float[] apoli$colours = AttachmentParts.newColours();
    @Unique
    private final float[] apoli$mixed = new float[4];
    @Unique
    private boolean apoli$prepared;
    @Unique
    private boolean apoli$tinted;
    @Unique
    private boolean apoli$tagged;
    @Unique
    private int apoli$hidden;
    @Unique
    private int apoli$group;
    @Unique
    private int apoli$bits;
    @Unique
    @Nullable
    private EarsFeatures apoli$features;
    @Unique
    @Nullable
    private EarsFeatureType apoli$feature;
    @Unique
    private int apoli$quad;

    @Override
    public void apoli$begin(EarsFeatures features) {
        apoli$feature = null;
        apoli$quad = 0;
        apoli$group = 0;
        apoli$bits = 0;
        apoli$tagged = false;
        if (!(this.peer instanceof LivingEntity entity)) {
            apoli$prepared = false;
            return;
        }
        apoli$features = features;
        apoli$whole = SkinRenderCompat.rgba(entity);
        apoli$hidden = AttachmentParts.hiddenMask(entity);
        apoli$tinted = AttachmentParts.colours(ClientDisguiseManager.powerSource(entity), apoli$colours);
        apoli$prepared = true;
    }

    @Override
    public void apoli$end() {
        apoli$prepared = false;
        apoli$feature = null;
        apoli$features = null;
    }

    @Override
    public void apoli$feature(@Nullable EarsFeatureType feature) {
        apoli$feature = feature;
        apoli$quad = 0;
        apoli$group = 0;
        apoli$bits = 0;
    }

    @Inject(method = "tag(Ljava/lang/String;)V", at = @At("HEAD"), require = 0)
    private void apoli$trackTag(String tag, CallbackInfo ci) {
        apoli$tagged = true;
        apoli$group = EarsAttachments.tagBits(tag);
        apoli$bits = apoli$group;
    }

    @Inject(method = "subtag(Ljava/lang/String;)V", at = @At("HEAD"), require = 0)
    private void apoli$trackSubtag(String subtag, CallbackInfo ci) {
        if (apoli$group != 0) apoli$bits = EarsAttachments.subtagBits(apoli$group, apoli$bits, subtag);
    }

    @Inject(method = {RENDER_FRONT, RENDER_BACK}, at = @At("HEAD"), cancellable = true)
    private void apoli$beginFlatQuad(CallbackInfo ci) {
        if (!EarsAttachments.SKEWED_QUADS && apoli$beginQuad()) ci.cancel();
    }

    @Inject(method = {RENDER_FRONT_SKEW, RENDER_BACK_SKEW}, at = @At("HEAD"), cancellable = true, require = 0)
    private void apoli$beginSkewedQuad(CallbackInfo ci) {
        if (apoli$beginQuad()) ci.cancel();
    }

    @ModifyArg(method = {RENDER_FRONT, RENDER_BACK, RENDER_FRONT_SKEW, RENDER_BACK_SKEW},
               at = @At(value = "INVOKE", target = ADD_VERTEX), index = 3, require = 0)
    private float apoli$tintRed(float red) {
        return red * apoli$colour[0];
    }

    @ModifyArg(method = {RENDER_FRONT, RENDER_BACK, RENDER_FRONT_SKEW, RENDER_BACK_SKEW},
               at = @At(value = "INVOKE", target = ADD_VERTEX), index = 4, require = 0)
    private float apoli$tintGreen(float green) {
        return green * apoli$colour[1];
    }

    @ModifyArg(method = {RENDER_FRONT, RENDER_BACK, RENDER_FRONT_SKEW, RENDER_BACK_SKEW},
               at = @At(value = "INVOKE", target = ADD_VERTEX), index = 5, require = 0)
    private float apoli$tintBlue(float blue) {
        return blue * apoli$colour[2];
    }

    @ModifyArg(method = {RENDER_FRONT, RENDER_BACK, RENDER_FRONT_SKEW, RENDER_BACK_SKEW},
               at = @At(value = "INVOKE", target = ADD_VERTEX), index = 6, require = 0)
    private float apoli$tintAlpha(float alpha) {
        return alpha * apoli$colour[3];
    }

    @Unique
    private boolean apoli$beginQuad() {
        if (!apoli$prepared) {
            apoli$colour = this.peer instanceof LivingEntity entity
                ? SkinRenderCompat.rgba(entity)
                : ModelColorPower.IDENTITY;
            return false;
        }
        int bit = apoli$tagged ? apoli$bits : EarsAttachments.bit(apoli$feature, apoli$features, apoli$quad++);
        if (bit != 0 && (apoli$hidden & bit) != 0) return true;
        if (bit == 0 || !apoli$tinted) {
            apoli$colour = apoli$whole;
            return false;
        }
        int at = BodyAttachments.index(bit) * AttachmentParts.STRIDE;
        float[] whole = apoli$whole;
        float[] colours = apoli$colours;
        float[] mixed = apoli$mixed;
        mixed[0] = whole[0] * colours[at];
        mixed[1] = whole[1] * colours[at + 1];
        mixed[2] = whole[2] * colours[at + 2];
        mixed[3] = whole[3] * colours[at + 3];
        apoli$colour = mixed;
        return false;
    }
}
