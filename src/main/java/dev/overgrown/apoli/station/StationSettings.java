package dev.overgrown.apoli.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

public record StationSettings(
    Optional<Component> title,
    Key key,
    float cookSpeed,
    float fuelEfficiency,
    FuelMode fuel,
    float fuelCost,
    Optional<ResourceLocation> fuelResource,
    int fuelBurnTime,
    boolean tickWhileClosed,
    Optional<EntityAction> finishAction,
    boolean dropOnDeath,
    Optional<ItemCondition> dropOnDeathFilter,
    boolean recoverable
) {
    public static final MapCodec<StationSettings> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        TextComponent.CODEC.optionalFieldOf("title").forGetter(StationSettings::title),
        Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(StationSettings::key),
        ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("cook_speed", 1.0F).forGetter(StationSettings::cookSpeed),
        ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("fuel_efficiency", 1.0F).forGetter(StationSettings::fuelEfficiency),
        FuelMode.CODEC.optionalFieldOf("fuel", FuelMode.ITEMS).forGetter(StationSettings::fuel),
        Codec.floatRange(0.0F, Float.MAX_VALUE).optionalFieldOf("fuel_cost", 1.0F).forGetter(StationSettings::fuelCost),
        IdCodecs.ID.optionalFieldOf("fuel_resource").forGetter(StationSettings::fuelResource),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("fuel_burn_time", 200).forGetter(StationSettings::fuelBurnTime),
        Codec.BOOL.optionalFieldOf("tick_while_closed", true).forGetter(StationSettings::tickWhileClosed),
        LoggedOptionalField.strict("finish_action", EntityAction.CODEC).forGetter(StationSettings::finishAction),
        Codec.BOOL.optionalFieldOf("drop_on_death", false).forGetter(StationSettings::dropOnDeath),
        LoggedOptionalField.strict("drop_on_death_filter", ItemCondition.CODEC).forGetter(StationSettings::dropOnDeathFilter),
        Codec.BOOL.optionalFieldOf("recoverable", true).forGetter(StationSettings::recoverable)
    ).apply(i, StationSettings::new));
}
