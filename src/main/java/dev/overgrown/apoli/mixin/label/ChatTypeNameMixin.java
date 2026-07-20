package dev.overgrown.apoli.mixin.label;

import dev.overgrown.apoli.entity.DisplayNameOverrides;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatType.class)
public abstract class ChatTypeNameMixin {

    @Inject(
        method = "bind(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/ChatType$Bound;",
        at = @At("HEAD"), cancellable = true)
    private static void apoli$overrideChatName(ResourceKey<ChatType> key, Entity entity,
                                               CallbackInfoReturnable<ChatType.Bound> cir) {
        Component override = DisplayNameOverrides.chatNameFor(entity);
        if (override != null) {
            cir.setReturnValue(ChatType.bind(key, entity.level().registryAccess(), override));
        }
    }
}
