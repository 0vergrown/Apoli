package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

// Skips the death screen for whoever has it, like the doImmediateRespawn gamerule but for one player. The client
// keeps that setting per player and the server sends it on its own, so it's sent again right before each death
// with this player's value.
public final class InstantRespawnPower extends PowerType<EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static void beforeDeath(ServerPlayer player) {
        if (player.connection == null) return;
        boolean immediate = player.level().getGameRules().getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN)
            || PowerLookup.hasActive(player, ApoliIds.INSTANT_RESPAWN);
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN,
            immediate ? 1.0F : 0.0F));
    }
}
