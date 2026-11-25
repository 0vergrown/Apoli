package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.station.FurnaceStation;
import dev.overgrown.apoli.station.FurnaceType;
import dev.overgrown.apoli.station.StationContainer;
import dev.overgrown.apoli.station.StationSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;

import java.util.UUID;

public final class FurnacePower extends StationPower<FurnacePower.Config> {
    public record Config(FurnaceType furnaceType, StationSettings settings) implements StationPower.Config {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            FurnaceType.CODEC.optionalFieldOf("furnace_type", FurnaceType.FURNACE).forGetter(Config::furnaceType),
            StationSettings.MAP_CODEC.forGetter(Config::settings)
        ).apply(i, Config::new));
    }

    @Override
    protected StationContainer create(UUID owner, ResourceLocation powerId, Config cfg) {
        return new FurnaceStation(owner, powerId, cfg.furnaceType());
    }

    @Override
    protected boolean matches(StationContainer station, Config cfg) {
        return station instanceof FurnaceStation furnace && furnace.type() == cfg.furnaceType();
    }

    @Override
    protected boolean tickStation(StationContainer station, Config cfg, ServerPlayer player) {
        return station instanceof FurnaceStation furnace && furnace.tick(player.serverLevel(), player, cfg.settings());
    }

    @Override
    protected MenuProvider menuProvider(StationContainer station, Config cfg, Component title) {
        return new SimpleMenuProvider((id, inv, p) -> cfg.furnaceType().createMenu(id, inv, station, station.data()), title);
    }

    @Override
    protected Component defaultTitle(Config cfg) {
        return cfg.furnaceType().defaultTitle();
    }
}
