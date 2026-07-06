package dev.overgrown.apoli.mixin.player_model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.ApoliPlayerModels;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class PlayerRendererModelSwapMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    @Unique
    private PlayerModel<AbstractClientPlayer> apoli$cachedModel;

    @Unique
    private boolean apoli$slimVariant;

    protected PlayerRendererModelSwapMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadow) {
        super(ctx, model, shadow);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void apoli$bakeModels(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        this.apoli$slimVariant = slim;
        ApoliPlayerModels.bake(ctx, slim);
    }

    @Inject(
        method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private void apoli$swapModelPre(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose,
                                    MultiBufferSource buffers, int light, CallbackInfo ci) {
        this.apoli$cachedModel = this.model;
        this.model = ApoliPlayerModels.override(player, this.model, this.apoli$slimVariant);
    }

    @Inject(
        method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("RETURN")
    )
    private void apoli$swapModelPost(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose,
                                     MultiBufferSource buffers, int light, CallbackInfo ci) {
        this.model = this.apoli$cachedModel;
    }
}
