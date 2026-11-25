package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.GuiPower;
import dev.overgrown.apoli.power.builtin.StationPower;
import dev.overgrown.apoli.station.PortableStation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class OpenStationAction implements ActionType<EntityCtx, OpenStationAction.Cfg> {
    public record Cfg(Optional<PortableStation> station, Optional<ResourceLocation> power, Optional<Component> title) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            PortableStation.CODEC.optionalFieldOf("station").forGetter(Cfg::station),
            IdCodecs.ID.optionalFieldOf("power").forGetter(Cfg::power),
            TextComponent.CODEC.optionalFieldOf("title").forGetter(Cfg::title)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        if (cfg.power.isPresent()) {
            if (StationPower.open(cfg.power.get(), player)) return;
            ResourceLocation gui = GuiPower.resolve(PowerContainer.of(player), cfg.power.get());
            if (gui != null) GuiPower.open(gui, player);
            return;
        }
        if (cfg.station.isEmpty()) return;
        PortableStation station = cfg.station.get();
        player.openMenu(station.menuProvider(player, cfg.title.orElseGet(station::defaultTitle)));
    }
}
