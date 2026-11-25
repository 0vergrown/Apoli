package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.station.BrewingStation;
import dev.overgrown.apoli.station.StationContainer;
import dev.overgrown.apoli.station.StationSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;

import java.util.UUID;

public final class BrewingStandPower extends StationPower<BrewingStandPower.Config> {
    private static final Component DEFAULT_TITLE = Component.translatable("container.brewing");

    public record Config(StationSettings settings) implements StationPower.Config {}

    @Override
    public MapCodec<Config> configCodec() {
        return StationSettings.MAP_CODEC.xmap(Config::new, Config::settings);
    }

    @Override
    protected StationContainer create(UUID owner, ResourceLocation powerId, Config cfg) {
        return new BrewingStation(owner, powerId);
    }

    @Override
    protected boolean matches(StationContainer station, Config cfg) {
        return station instanceof BrewingStation;
    }

    @Override
    protected boolean tickStation(StationContainer station, Config cfg, ServerPlayer player) {
        return station instanceof BrewingStation brewing && brewing.tick(player.serverLevel(), player, cfg.settings());
    }

    @Override
    protected MenuProvider menuProvider(StationContainer station, Config cfg, Component title) {
        return new SimpleMenuProvider((id, inv, p) -> {
            station.startOpen(p);
            return new BrewingStandMenu(id, inv, station, station.data()) {
                @Override
                public void removed(Player player) {
                    super.removed(player);
                    station.stopOpen(player);
                }
            };
        }, title);
    }

    @Override
    protected Component defaultTitle(Config cfg) {
        return DEFAULT_TITLE;
    }
}
