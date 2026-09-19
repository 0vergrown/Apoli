package dev.overgrown.apoli.mixin.effects;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.overgrown.apoli.effects.CustomMobEffect;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MobEffectTextureManager.class)
public class MobEffectTextureManagerCustomEffectSpriteMixin {

    @WrapMethod(method = "get")
    TextureAtlasSprite apoli$getCustomSprite(Holder<MobEffect> holder, Operation<TextureAtlasSprite> original) {
        if (holder.value() instanceof CustomMobEffect effect) {

            if (effect.icon.isPresent()) {
                return ((TextureAtlasHolderInvoker) this).apoli$invokeGetSprite(effect.icon.get());
            }
        }

        return original.call(holder);
    }
}
