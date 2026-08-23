package dev.overgrown.apoli.mixin.reach;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.attribute.ApoliAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.util.Mth;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeReachMixin {

    @Shadow
    protected ServerPlayer player;

    @ModifyExpressionValue(
        method = "handleBlockBreakAction",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;MAX_INTERACTION_DISTANCE:D",
            opcode = Opcodes.GETSTATIC))
    private double apoli$blockBreakRange(double original) {
        return Mth.square(ApoliAttributes.blockInteractionRange(this.player) + 1.5);
    }
}
