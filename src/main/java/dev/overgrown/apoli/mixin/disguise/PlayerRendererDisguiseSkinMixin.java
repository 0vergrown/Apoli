package dev.overgrown.apoli.mixin.disguise;

import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerRendererDisguiseSkinMixin {

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void apoli$disguiseSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        DisguiseData data = ClientDisguiseManager.get(player.getId());
        if (data == null || !data.isPlayerDisguise() || data.playerUuid().isEmpty()) return;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        PlayerInfo info = connection.getPlayerInfo(data.playerUuid().get());
        if (info != null) cir.setReturnValue(info.getSkin().texture());
    }
}