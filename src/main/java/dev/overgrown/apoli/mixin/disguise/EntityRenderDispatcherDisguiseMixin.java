package dev.overgrown.apoli.mixin.disguise;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = EntityRenderDispatcher.class, priority = 900)
@Environment(EnvType.CLIENT)
public abstract class EntityRenderDispatcherDisguiseMixin {

    @Shadow
    private Map<String, EntityRenderer<? extends Player>> playerRenderers;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void apoli$renderDisguise(E entity, double x, double y, double z, float yaw, float partialTick,
                                                         PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        DisguiseData data = ClientDisguiseManager.get(entity.getId());
        if (data == null) return;
        if (data.isPlayerDisguise() && entity instanceof Player) return;

        Entity dummy = ClientDisguiseManager.syncedDummy(entity.getId(), entity);
        if (dummy == null) return;

        ClientDisguiseManager.ensureCustomModelLayer((EntityRenderDispatcher) (Object) this, dummy);
        Entity previous = ClientDisguiseManager.beginDisguiseRender(entity);
        try {
            ((EntityRenderDispatcher) (Object) this).render(dummy, x, y, z, yaw, partialTick, pose, buffers, light);
        } finally {
            ClientDisguiseManager.endDisguiseRender(previous);
        }
        ci.cancel();
    }

    @ModifyReturnValue(method = "getRenderer", at = @At("RETURN"))
    private EntityRenderer<?> apoli$disguiseRenderer(EntityRenderer<?> original, Entity entity) {
        if (!(entity instanceof AbstractClientPlayer)) return original;
        PlayerInfo info = ClientDisguiseManager.playerInfo(entity.getId());
        if (info == null) return original;
        EntityRenderer<? extends Player> renderer = this.playerRenderers.get(info.getModelName());
        return renderer == null ? original : renderer;
    }
}
