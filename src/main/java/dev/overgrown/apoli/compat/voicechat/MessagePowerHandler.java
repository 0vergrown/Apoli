package dev.overgrown.apoli.compat.voicechat;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;

public final class MessagePowerHandler {

    private MessagePowerHandler() {}

    public static void handle(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        if (sender == null) return;
        try {
            run(event, sender);
        } catch (Throwable t) {
            Apoli.LOGGER.error("[Apoli] action_on_sending_message failed while handling \"{}\" from {}; "
                + "letting the message through.", event.getRawText(), sender.getName().getString(), t);
        }
    }

    private static void run(ServerChatEvent event, ServerPlayer sender) {
        ActionOnSendingMessagePower.Result result = ActionOnSendingMessagePower.fire(
            sender, event.getRawText(), ChatType.CHAT.location());

        if (result.prevented()) {
            event.setCanceled(true);
            return;
        }
        if (result.message() != null) {
            event.setMessage(Component.literal(result.message()));
        }
    }
}
