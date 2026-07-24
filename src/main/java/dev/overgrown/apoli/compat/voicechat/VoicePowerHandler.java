package dev.overgrown.apoli.compat.voicechat;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ActionOnReplyPower;
import dev.overgrown.apoli.power.builtin.ActionOnSpeakPower;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class VoicePowerHandler {
    private VoicePowerHandler() {}

    public static void onSpeakStart(MinecraftServer server, UUID uuid) {
        ServerPlayer speaker = server.getPlayerList().getPlayer(uuid);
        if (speaker == null) {
            return;
        }
        ServerLevel level = speaker.serverLevel();
        PowerLookup.forEach(speaker, ApoliIds.ACTION_ON_SPEAK, ActionOnSpeakPower.Config.class, cfg ->
            cfg.actionOnSpeak().ifPresent(action -> action.run(new EntityCtx(speaker, level))));

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
        PowerLookup.forEach(speaker, ApoliIds.ACTION_ON_SPEAK, ActionOnSpeakPower.Config.class, cfg ->
            cfg.actionOnStopSpeaking().ifPresent(action -> action.run(new EntityCtx(speaker, level))));
    }
}
