package dev.overgrown.apoli.mixin.flag;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ElytraFlightPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ElytraLayer.class)
@Environment(EnvType.CLIENT)
public abstract class ElytraLayerRenderMixin {
    @Unique
    private LivingEntity apoli$entity;

    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean apoli$forceElytraRender(ItemStack stack, Item item,
                                            PoseStack poseStack, MultiBufferSource buffer, int light,
                                            LivingEntity entity, float f, float g, float h, float j, float k, float l) {
        this.apoli$entity = entity;
        if (!entity.isInvisible() && apoli$shouldRenderElytra(entity)) {
            return true;
        }
        return stack.is(item);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private ResourceLocation apoli$elytraTexture(ResourceLocation original) {
        ResourceLocation[] texture = new ResourceLocation[]{null};
        if (apoli$entity != null) {
            PowerLookup.forEach(apoli$entity, Apoli.id("elytra_flight"), ElytraFlightPower.Config.class, cfg -> {
                if (texture[0] == null) cfg.textureLocation().ifPresent(t -> texture[0] = t);
            });
        }
        return texture[0] != null ? texture[0] : original;
    }

    @Unique
    private static boolean apoli$shouldRenderElytra(LivingEntity entity) {
        boolean[] render = new boolean[]{false};
        PowerLookup.forEach(entity, Apoli.id("elytra_flight"), ElytraFlightPower.Config.class, cfg -> {
            if (cfg.renderElytra()) render[0] = true;
        });
        return render[0];
    }
}
