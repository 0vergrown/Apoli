package dev.overgrown.apoli.network.payload;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class AuxValues {

    private AuxValues() {}

    public static Object2DoubleOpenHashMap<ResourceLocation> newMap(int expected) {
        Object2DoubleOpenHashMap<ResourceLocation> map = new Object2DoubleOpenHashMap<>(expected);
        map.defaultReturnValue(Double.NaN);
        return map;
    }

    public static Object2DoubleMap<ResourceLocation> copy(Object2DoubleMap<ResourceLocation> source) {
        Object2DoubleOpenHashMap<ResourceLocation> copy = newMap(source.size());
        copy.putAll(source);
        return copy;
    }

    public static void write(FriendlyByteBuf buf, Object2DoubleMap<ResourceLocation> values) {
        buf.writeVarInt(values.size());
        for (Object2DoubleMap.Entry<ResourceLocation> entry : Object2DoubleMaps.fastIterable(values)) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeDouble(entry.getDoubleValue());
        }
    }

    public static Object2DoubleMap<ResourceLocation> read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Object2DoubleOpenHashMap<ResourceLocation> values = newMap(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation key = buf.readResourceLocation();
            values.put(key, buf.readDouble());
        }
        return values;
    }

    public static void writeTables(FriendlyByteBuf buf, Map<ResourceLocation, double[]> tables) {
        buf.writeVarInt(tables.size());
        for (Map.Entry<ResourceLocation, double[]> entry : tables.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            double[] values = entry.getValue();
            buf.writeVarInt(values.length);
            for (double value : values) buf.writeDouble(value);
        }
    }

    public static Map<ResourceLocation, double[]> readTables(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<ResourceLocation, double[]> tables = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation key = buf.readResourceLocation();
            int length = buf.readVarInt();
            double[] values = new double[length];
            for (int slot = 0; slot < length; slot++) values[slot] = buf.readDouble();
            tables.put(key, values);
        }
        return tables;
    }
}
