package dev.overgrown.apoli.compat.voicechat;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerStateChangedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

import java.util.UUID;

public final class ApoliVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return "apoli";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(PlayerStateChangedEvent.class, this::onStateChanged);
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        VoiceState.setDisabledCheck(uuid -> {
            VoicechatConnection connection = api.getConnectionOf(uuid);
            return connection != null && (connection.isDisabled() || !connection.isConnected());
        });
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection connection = event.getSenderConnection();
        if (connection == null) {
            return;
        }
        ServerPlayer player = connection.getPlayer();
        if (player == null) {
            return;
        }
        byte[] data = event.getPacket().getOpusEncodedData();
        VoiceState.micPacketAsync(player.getUuid(), data == null ? 0 : data.length);
    }

    private void onStateChanged(PlayerStateChangedEvent event) {
        UUID uuid = event.getPlayerUuid();
        if (uuid == null) {
            return;
        }
        VoiceState.stateChangedAsync(uuid, event.isDisabled(), event.isDisconnected());
    }
}
