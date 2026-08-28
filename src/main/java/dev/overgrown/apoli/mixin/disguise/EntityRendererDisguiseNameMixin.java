package dev.overgrown.apoli.mixin.disguise;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class EntityRendererDisguiseNameMixin<T extends Entity> {

    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void apoli$hideDisguiseName(T entity, Component name, PoseStack pose,
                                        MultiBufferSource buffers, int light, float partialTick, CallbackInfo ci) {
        if (ClientDisguiseManager.hidesName(entity)) ci.cancel();
    }

    @ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true)
    private Component apoli$disguiseName(Component original, T entity, Component name, PoseStack pose,
                                        MultiBufferSource buffers, int light, float partialTick) {
        DisguiseData data = ClientDisguiseManager.get(entity.getId());
        if (data == null) return original;
        if (data.name().isPresent()) {

            String disguiseName = data.name().get();
            return disguiseName.isEmpty() ? original : Component.literal(disguiseName);
        }
        if (data.isPlayerDisguise()) return original;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(data.entityTypeId());
        return type != null ? Component.translatable(type.getDescriptionId()) : original;
    }
}
