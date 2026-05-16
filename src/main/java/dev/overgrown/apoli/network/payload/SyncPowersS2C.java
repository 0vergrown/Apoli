package dev.overgrown.apoli.network.payload;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncPowersS2C(Map<ResourceLocation, String> rawPowers) {
    public static final ResourceLocation CHANNEL = Apoli.id("sync_powers");

    public static SyncPowersS2C fromCurrent() {
        Map<ResourceLocation, String> out = new HashMap<>();
        for (Map.Entry<ResourceLocation, Power> e : ApoliPowers.view().entrySet()) {
            Power.CODEC.encodeStart(JsonOps.INSTANCE, e.getValue()).result()
                .ifPresent(json -> out.put(e.getKey(), json.toString()));
        }
        return new SyncPowersS2C(out);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(rawPowers.size());
        for (Map.Entry<ResourceLocation, String> e : rawPowers.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeUtf(e.getValue());
        }
    }

    public static SyncPowersS2C read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<ResourceLocation, String> out = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            String json = buf.readUtf();
            out.put(id, json);
        }
        return new SyncPowersS2C(out);
    }

    public Map<ResourceLocation, Power> decodeAll() {
        Map<ResourceLocation, Power> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, String> e : rawPowers.entrySet()) {
            JsonElement el = JsonParser.parseString(e.getValue());
            Power.CODEC.parse(JsonOps.INSTANCE, el).result().ifPresent(p -> result.put(e.getKey(), p));
        }
        return result;
    }
}