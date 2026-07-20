package dev.overgrown.apoli.mixin.label;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.ClientLabelState;
import dev.overgrown.apoli.power.builtin.ModifyLabelRenderPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderer.class, priority = 1100)
@Environment(EnvType.CLIENT)
public abstract class EntityRendererLabelMixin<T extends Entity> {

    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void apoli$hideLabel(T entity, Component name, PoseStack pose, MultiBufferSource buffers,
                                 int light, CallbackInfo ci) {
        ClientLabelState.Pick pick = ClientLabelState.pick(entity, Minecraft.getInstance().getCameraEntity());
        if (pick != null && pick.mode() == ModifyLabelRenderPower.LabelMode.HIDE_COMPLETELY) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true)
    private Component apoli$replaceLabel(Component original, T entity, Component name, PoseStack pose,
                                         MultiBufferSource buffers, int light) {
        ClientLabelState.Pick pick = ClientLabelState.pick(entity, Minecraft.getInstance().getCameraEntity());
        return pick != null && pick.text() != null ? pick.text() : original;
    }

    @ModifyVariable(method = "renderNameTag", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private boolean apoli$partialHide(boolean visible, T entity, Component name, PoseStack pose,
                                      MultiBufferSource buffers, int light) {
        if (!visible) return false;
        ClientLabelState.Pick pick = ClientLabelState.pick(entity, Minecraft.getInstance().getCameraEntity());
        return pick == null || pick.mode() != ModifyLabelRenderPower.LabelMode.HIDE_PARTIALLY;
    }
}
