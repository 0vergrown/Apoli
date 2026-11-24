package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.station.FurnaceStation;
import dev.overgrown.apoli.station.FurnaceType;
import dev.overgrown.apoli.station.StationContainer;
import dev.overgrown.apoli.station.StationSettings;
import dev.overgrown.apoli.station.StationStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import org.jetbrains.annotations.Nullable;

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
    protected @Nullable StationContainer station(ResourceLocation powerId, Config cfg, ServerPlayer player,
                                                 PowerContainerImpl holder, boolean create) {
        FurnaceStation station = StationStorage.get(holder, player.getUUID(), powerId, FurnaceStation.class,
            () -> new FurnaceStation(player.getUUID(), powerId, cfg.furnaceType()), create);
        if (station == null || station.type() == cfg.furnaceType()) return station;
        StationStorage.flush(station, holder);
        StationStorage.discard(player.getUUID(), powerId);
        return StationStorage.get(holder, player.getUUID(), powerId, FurnaceStation.class,
            () -> new FurnaceStation(player.getUUID(), powerId, cfg.furnaceType()), create);
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
