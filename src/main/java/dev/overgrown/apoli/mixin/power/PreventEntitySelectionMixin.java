package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.client.render.ClientRenderFlags;
import dev.overgrown.apoli.power.builtin.PreventEntitySelectionPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class PreventEntitySelectionMixin {
    @ModifyExpressionValue(method = "lambda$pick$57(Lnet/minecraft/world/entity/Entity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isPickable()Z"))
    private static boolean apoli$preventEntitySelection(boolean original, Entity entity) {
        if (!original || !ClientRenderFlags.has(ClientRenderFlags.PREVENT_ENTITY_SELECTION)) return original;
        LocalPlayer viewer = Minecraft.getInstance().player;
        if (viewer == null) return true;
        return !PreventEntitySelectionPower.prevents(viewer, entity);
    }
}
