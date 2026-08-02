package dev.overgrown.apoli.mixin.disguise;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class PlayerRendererDisguiseSkinMixin {

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void apoli$disguiseSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation skin = apoli$disguiseSkinOf(player);
        if (skin != null) cir.setReturnValue(skin);
    }

    @ModifyExpressionValue(method = "renderHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/PlayerSkin;texture()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation apoli$disguiseHandSkin(ResourceLocation original, PoseStack pose, MultiBufferSource buffers,
                                                    int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve) {
        ResourceLocation skin = apoli$disguiseSkinOf(player);
        return skin != null ? skin : original;
    }

    @Unique
    private ResourceLocation apoli$disguiseSkinOf(AbstractClientPlayer player) {
        DisguiseData data = ClientDisguiseManager.get(player.getId());
        if (data == null || !data.isPlayerDisguise() || data.playerUuid().isEmpty()) return null;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return null;
        PlayerInfo info = connection.getPlayerInfo(data.playerUuid().get());
        return info == null ? null : info.getSkin().texture();
    }
}
