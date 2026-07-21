package dev.overgrown.apoli.mixin.disguise;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderDispatcher.class, priority = 900)
@OnlyIn(Dist.CLIENT)
public abstract class EntityRenderDispatcherDisguiseMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void apoli$renderDisguise(E entity, double x, double y, double z, float yaw, float partialTick,
                                                         PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) return;

        DisguiseData data = ClientDisguiseManager.get(entity.getId());
        if (data == null || data.isPlayerDisguise()) return;

        Entity dummy = ClientDisguiseManager.syncedDummy(entity.getId(), entity);
        if (dummy == null) return;

        ((EntityRenderDispatcher) (Object) this).render(dummy, x, y, z, yaw, partialTick, pose, buffers, light);
        ci.cancel();
    }
}
