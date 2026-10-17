package dev.overgrown.apoli.mixin.disguise;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerRendererDisguiseSkinMixin {

    @ModifyReturnValue(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;", at = @At("RETURN"))
    private ResourceLocation apoli$disguiseSkin(ResourceLocation original, AbstractClientPlayer player) {
        ResourceLocation skin = apoli$disguiseSkinOf(player);
        if (skin == null || CustomModelRenderPower.firstReplace(player) != null) return original;
        return skin;
    }

    @ModifyExpressionValue(method = "renderHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkinTextureLocation()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation apoli$disguiseHandSkin(ResourceLocation original, PoseStack pose, MultiBufferSource buffers,
                                                    int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve) {
        ResourceLocation skin = apoli$disguiseSkinOf(player);
        return skin != null ? skin : original;
    }

    @Unique
    private ResourceLocation apoli$disguiseSkinOf(AbstractClientPlayer player) {
        net.minecraft.client.multiplayer.PlayerInfo info = ClientDisguiseManager.playerInfo(player.getId());
        return info == null ? null : info.getSkinLocation();
    }
}
