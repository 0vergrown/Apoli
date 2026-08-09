package dev.overgrown.apoli.compat.voicechat;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class MessagePowerHandler {

    private MessagePowerHandler() {}

    public static boolean handle(@Nullable Entity holder, @Nullable ServerPlayer speaker,
                                 String content, ChatType.Bound params) {
        if (holder == null) return true;
        try {
            return run(holder, speaker, content, params);
        } catch (Throwable t) {
            Apoli.LOGGER.error("[Apoli] action_on_sending_message failed while handling \"{}\" from {}; "
                + "letting the message through.", content, holder.getName().getString(), t);
            return true;
        }
    }

    private static boolean run(Entity holder, @Nullable ServerPlayer speaker,
                               String content, ChatType.Bound params) {
        ResourceLocation typeId = params.chatType().unwrapKey().map(ResourceKey::location).orElse(null);

        ActionOnSendingMessagePower.Result result =
            ActionOnSendingMessagePower.fire(holder, content, typeId);

        if (result.prevented()) {
            return false;
        }
        String replaced = result.message();
        if (replaced == null) {
            return true;
        }

        MinecraftServer server = holder.getServer();
        if (server == null) {
            return true;
        }
        Component decorated = params.decorate(Component.literal(replaced));
        server.getPlayerList().broadcastSystemMessage(decorated, false);
        if (speaker != null && !server.getPlayerList().getPlayers().contains(speaker)) {
            speaker.sendSystemMessage(decorated);
        }
        return false;
    }
}
