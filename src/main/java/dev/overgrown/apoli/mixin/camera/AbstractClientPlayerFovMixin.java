package dev.overgrown.apoli.mixin.camera;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.client.ClientResolvedPowers;
import dev.overgrown.apoli.power.builtin.ModifyFovPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
@OnlyIn(Dist.CLIENT)
public abstract class AbstractClientPlayerFovMixin {

    @ModifyReturnValue(method = "getFieldOfViewModifier", at = @At(value = "RETURN", ordinal = 0))
    private float apoli$modifyScopedFov(float original) {
        if (ClientResolvedPowers.isEmpty()) return original;
        return ModifyFovPower.apply((AbstractClientPlayer) (Object) this, original, ClientResolvedPowers::isActive);
    }

    @WrapOperation(method = "getFieldOfViewModifier",
        at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;getFieldOfViewModifier(Lnet/minecraft/world/entity/player/Player;F)F"))
    private float apoli$modifyFov(Player player, float fov, Operation<Float> original) {
        if (ClientResolvedPowers.isEmpty()) return original.call(player, fov);
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        float modified = ModifyFovPower.apply(self, fov, ClientResolvedPowers::isActive);
        float result = original.call(player, modified);
        if (!ModifyFovPower.ignoresEffectScale(self, ClientResolvedPowers::isActive)) return result;
        float scale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        if (scale <= 0.0F) return modified;
        return 1.0F + (result - 1.0F) / scale;
    }
}
