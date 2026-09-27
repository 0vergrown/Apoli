package dev.overgrown.apoli.compat.voicechat;

import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class VoicePowerHandler {
    private VoicePowerHandler() {}

    public static void onSpeakStart(MinecraftServer server, UUID uuid) {
        ServerPlayer speaker = server.getPlayerList().getPlayer(uuid);
        if (speaker == null) {
            return;
        }
        ServerLevel level = speaker.serverLevel();
        PowerLookup.forEach(speaker, ApoliIds.ACTION_ON_SPEAK, ActionOnSpeakPower.Config.class, cfg -> {
            cfg.actionOnSpeak().ifPresent(action -> action.run(new EntityCtx(speaker, level)));
            cfg.bientityActionOnSpeak().ifPresent(action ->
                runOnListeners(server, speaker, level, action, cfg.bientityCondition()));
        });

        for (ServerPlayer actor : server.getPlayerList().getPlayers()) {
            if (actor == speaker || actor.level() != speaker.level()) {
                continue;
            }
            PowerLookup.forEach(actor, ApoliIds.ACTION_ON_REPLY, ActionOnReplyPower.Config.class, cfg -> {
                if (VoiceState.ticksSinceSpoke(actor.getUUID()) > cfg.window()) {
                    return;
                }
                double range = cfg.range();
                if (actor.distanceToSqr(speaker) > range * range) {
                    return;
                }
                cfg.bientityAction().ifPresent(action ->
                    action.run(new BiEntityCtx(actor, speaker, actor.serverLevel())));
            });
        }
    }

    public static void onSpeakStop(MinecraftServer server, UUID uuid) {
        ServerPlayer speaker = server.getPlayerList().getPlayer(uuid);
        if (speaker == null) {
            return;
        }
        ServerLevel level = speaker.serverLevel();
        PowerLookup.forEach(speaker, ApoliIds.ACTION_ON_SPEAK, ActionOnSpeakPower.Config.class, cfg -> {
            cfg.actionOnStopSpeaking().ifPresent(action -> action.run(new EntityCtx(speaker, level)));
            cfg.bientityActionOnStopSpeaking().ifPresent(action ->
                runOnListeners(server, speaker, level, action, cfg.bientityCondition()));
        });
    }

    private static void runOnListeners(MinecraftServer server, ServerPlayer speaker, ServerLevel level,
                                       BiEntityAction action, Optional<BiEntityCondition> condition) {
        boolean whispering = VoiceState.isWhispering(speaker.getUUID());
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (int i = 0, n = players.size(); i < n; i++) {
            ServerPlayer listener = players.get(i);
            if (listener == speaker || listener.level() != level) continue;
            UUID listenerId = listener.getUUID();
            if (VoiceState.isDisabled(listenerId) || VoiceState.isDisconnected(listenerId)) continue;
            double reach = VoiceHearing.reach(listener, speaker, whispering);
            if (reach <= 0.0 || listener.distanceToSqr(speaker) > reach * reach) continue;
            BiEntityCtx ctx = BiEntityCtx.of(speaker, listener, level);
            if (condition.isPresent() && !condition.get().test(ctx)) continue;
            action.run(ctx);
        }
    }
}
