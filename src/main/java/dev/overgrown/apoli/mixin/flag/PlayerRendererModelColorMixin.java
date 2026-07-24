package dev.overgrown.apoli.mixin.flag;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.ModelColorState;
import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.ModelColorPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(PlayerRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class PlayerRendererModelColorMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    protected PlayerRendererModelColorMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadow) {
        super(ctx, model, shadow);
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"))
    private void apoli$rightHandColorSetup(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        apoli$setupHandColor(player, ModelParts.RIGHT_ARM);
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"))
    private void apoli$leftHandColorSetup(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        apoli$setupHandColor(player, ModelParts.LEFT_ARM);
    }

    @Inject(method = {"renderRightHand", "renderLeftHand"}, at = @At("RETURN"))
    private void apoli$handColorClear(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        ModelColorState.clear();
    }

    @Unique
    private void apoli$setupHandColor(AbstractClientPlayer player, String armName) {
        float[] whole = ModelColorPower.colorFor(player);
        Map<String, float[]> parts = ModelColorPower.partColorsFor(player);
        float[] pc = parts == null ? null : parts.get(armName);
        if (whole == ModelColorPower.IDENTITY && pc == null) return;

        float[] combined = pc == null
            ? whole
            : new float[]{whole[0] * pc[0], whole[1] * pc[1], whole[2] * pc[2], whole[3] * pc[3],
                Math.max(whole[4], pc[4])};

        Map<ModelPart, float[]> map = new IdentityHashMap<>();
        for (ModelPart part : ModelPartLookup.resolve(this.getModel(), armName)) {
            map.put(part, combined);
        }
        ModelColorState.set(map);
    }
}
