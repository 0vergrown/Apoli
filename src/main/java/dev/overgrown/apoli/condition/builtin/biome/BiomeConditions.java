package dev.overgrown.apoli.condition.builtin.biome;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ConditionTypes;

public final class BiomeConditions {
    private BiomeConditions() {}

    public static void register() {
        ConditionTypes.BIOME.register(Apoli.id("high_humidity"), new HighHumidityCondition());
        ConditionTypes.BIOME.register(Apoli.id("in_tag"), new InTagCondition());
        ConditionTypes.BIOME.register(Apoli.id("precipitation"), new PrecipitationCondition());
        ConditionTypes.BIOME.register(Apoli.id("temperature"), new TemperatureCondition());
    }
}
