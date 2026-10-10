package dev.overgrown.apoli.mixin.firstperson;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemInHandRenderer.class)
@Environment(EnvType.CLIENT)
public interface ItemInHandRendererAccessor {

    @Accessor("mainHandItem")
    ItemStack apoli$mainHandItem();

    @Accessor("offHandItem")
    ItemStack apoli$offHandItem();

    @Accessor("mainHandHeight")
    float apoli$mainHandHeight();

    @Accessor("oMainHandHeight")
    float apoli$oMainHandHeight();

    @Accessor("offHandHeight")
    float apoli$offHandHeight();

    @Accessor("oOffHandHeight")
    float apoli$oOffHandHeight();
}
