package dev.overgrown.apoli.mixin.reach;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.attribute.ApoliAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerReachAttributesMixin {

    @ModifyReturnValue(method = "createAttributes", at = @At("RETURN"))
    private static AttributeSupplier.Builder apoli$addReachAttributes(AttributeSupplier.Builder original) {
        return original
            .add(ApoliAttributes.BLOCK_INTERACTION_RANGE)
            .add(ApoliAttributes.ENTITY_INTERACTION_RANGE);
    }
}
