package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.client.render.ClientRenderFlags;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
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
        boolean[] prevented = {false};
        PowerLookup.forEach(viewer, ApoliIds.PREVENT_ENTITY_SELECTION, PreventEntitySelectionPower.Config.class, config -> {
            if (prevented[0]) return;
            if (config.entityCondition().isPresent()
                && !config.entityCondition().get().test(EntityCtx.of(entity, entity.level()))) return;
            if (config.bientityCondition().isPresent()
                && !config.bientityCondition().get().test(BiEntityCtx.of(viewer, entity, entity.level()))) return;
            prevented[0] = true;
        });
        return !prevented[0];
    }
}
