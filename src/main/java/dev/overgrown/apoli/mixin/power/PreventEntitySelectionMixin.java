package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PreventEntitySelectionPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(GameRenderer.class)
public class PreventEntitySelectionMixin {
    @ModifyExpressionValue(method = "lambda$pick$57(Lnet/minecraft/world/entity/Entity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isPickable()Z"))
    private static boolean apoli$preventEntitySelection(boolean original, Entity entity) {
        List<Boolean> result = new ArrayList<>();

        PowerLookup.forEach(Minecraft.getInstance().player, Apoli.id("prevent_entity_selection"), PreventEntitySelectionPower.Config.class, config ->
                result.add((config.entityCondition().isEmpty() || config.entityCondition().get().test(EntityCtx.of(entity, entity.level())))
                && (config.bientityCondition().isEmpty() || config.bientityCondition().get().test(BiEntityCtx.of(Minecraft.getInstance().player, entity, entity.level())))));

        return original && !result.contains(true);
    }
}