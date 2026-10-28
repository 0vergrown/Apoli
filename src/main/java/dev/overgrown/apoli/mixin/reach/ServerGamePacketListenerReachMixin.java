package dev.overgrown.apoli.mixin.reach;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.attribute.ApoliAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerReachMixin {

    private static final String MAX_INTERACTION_DISTANCE_FIELD =
        "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;MAX_INTERACTION_DISTANCE:D";

    @Shadow
    public ServerPlayer player;

    @ModifyExpressionValue(
        method = "handleInteract",
        at = @At(value = "FIELD", target = MAX_INTERACTION_DISTANCE_FIELD, opcode = Opcodes.GETSTATIC))
    private double apoli$entityInteractionRange(double original) {
        return ApoliAttributes.extendSquared(original, ApoliAttributes.entityInteractionBonus(this.player));
    }

    @ModifyExpressionValue(
        method = "handleUseItemOn",
        at = @At(value = "FIELD", target = MAX_INTERACTION_DISTANCE_FIELD, opcode = Opcodes.GETSTATIC))
    private double apoli$blockInteractionRange(double original) {
        return ApoliAttributes.extendSquared(original, ApoliAttributes.blockInteractionBonus(this.player));
    }

    @ModifyExpressionValue(method = "handleUseItemOn", at = @At(value = "CONSTANT", args = "doubleValue=64.0"))
    private double apoli$blockUseFeetRange(double original) {
        return ApoliAttributes.extendSquared(original, ApoliAttributes.blockInteractionBonus(this.player));
    }
}
