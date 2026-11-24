package dev.overgrown.apoli.station;

import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.server.level.ServerPlayer;

import java.util.OptionalDouble;

public final class StationFuel {
    private StationFuel() {}

    public static boolean pay(ServerPlayer holder, StationSettings settings) {
        return switch (settings.fuel()) {
            case ITEMS -> false;
            case NONE -> true;
            case HUNGER -> {
                if (holder.getAbilities().instabuild) yield true;
                if (holder.getFoodData().getFoodLevel() <= 0) yield false;
                holder.causeFoodExhaustion(settings.fuelCost());
                yield true;
            }
            case RESOURCE -> {
                if (settings.fuelResource().isEmpty()) yield false;
                PowerContainer container = PowerContainer.of(holder);
                OptionalDouble value = PowerResources.read(container, settings.fuelResource().get());
                if (value.isEmpty() || value.getAsDouble() < settings.fuelCost()) yield false;
                PowerResources.write(container, settings.fuelResource().get(), value.getAsDouble() - settings.fuelCost());
                yield true;
            }
        };
    }
}
