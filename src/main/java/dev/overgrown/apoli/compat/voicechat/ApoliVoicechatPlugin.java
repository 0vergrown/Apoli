package dev.overgrown.apoli.compat.voicechat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerStateChangedEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoiceDistanceEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;

import java.util.UUID;

@ForgeVoicechatPlugin
public final class ApoliVoicechatPlugin implements VoicechatPlugin {

    private volatile VoicechatServerApi api;

    @Override
    public String getPluginId() {
        return "apoli";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(PlayerStateChangedEvent.class, this::onStateChanged);
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
        registration.registerEvent(VoiceDistanceEvent.class, this::onVoiceDistance);
        registration.registerEvent(EntitySoundPacketEvent.class, this::onEntitySound);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi voicechat = event.getVoicechat();
        api = voicechat;
        VoiceState.setDisabledCheck(uuid -> {
            VoicechatConnection connection = voicechat.getConnectionOf(uuid);
            return connection != null && (connection.isDisabled() || !connection.isConnected());
        });
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        api = null;
        VoiceHearing.reset();
    }

    private void onClientSound(ClientSoundEvent event) {
        if (!SpeechAudioBus.hasSink()) return;
        short[] raw = event.getRawAudio();
        if (raw == null || raw.length == 0) return;
        SpeechAudioBus.push(raw.clone());
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
        MicrophonePacket packet = event.getPacket();
        byte[] data = packet.getOpusEncodedData();
        VoiceState.micPacketAsync(player.getUuid(), data == null ? 0 : data.length, packet.isWhispering());
    }

    private void onStateChanged(PlayerStateChangedEvent event) {
        UUID uuid = event.getPlayerUuid();
        if (uuid == null) {
            return;
        }
        VoiceState.stateChangedAsync(uuid, event.isDisabled(), event.isDisconnected());
    }

    private void onVoiceDistance(VoiceDistanceEvent event) {
        if (!VoiceHearing.isActive()) return;
        VoicechatConnection connection = event.getSenderConnection();
        if (connection == null) return;
        ServerPlayer speaker = connection.getPlayer();
        if (speaker == null) return;
        float base = event.getDistance();
        float widened = VoiceHearing.broadcastDistance(speaker.getUuid(), base, event.getPacket().isWhispering());
        if (widened > base) event.setDistance(widened);
    }

    private void onEntitySound(EntitySoundPacketEvent event) {
        if (!VoiceHearing.isActive()) return;
        if (!SoundPacketEvent.SOURCE_PROXIMITY.equals(event.getSource())) return;
        VoicechatServerApi voicechat = api;
        if (voicechat == null) return;
        VoicechatConnection receiverConnection = event.getReceiverConnection();
        if (receiverConnection == null) return;
        ServerPlayer listener = receiverConnection.getPlayer();
        if (listener == null) return;
        EntitySoundPacket packet = event.getPacket();
        boolean whispering = packet.isWhispering();
        float sent = packet.getDistance();
        float base = VoiceHearing.originalDistance(packet.getEntityUuid(), sent);
        double effective = VoiceHearing.rangeFor(listener.getUuid(), whispering, base);
        if (effective == sent) return;
        VoicechatConnection senderConnection = event.getSenderConnection();
        ServerPlayer speaker = senderConnection == null ? null : senderConnection.getPlayer();
        if (speaker == null) return;
        event.cancel();
        if (effective <= 0.0) return;
        if (distanceSquared(listener.getPosition(), speaker.getPosition()) > effective * effective) return;
        voicechat.sendEntitySoundPacketTo(receiverConnection,
            packet.entitySoundPacketBuilder().distance((float) effective).build());
    }

    private static double distanceSquared(Position a, Position b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
