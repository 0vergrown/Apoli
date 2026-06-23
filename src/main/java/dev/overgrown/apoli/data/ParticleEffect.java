package dev.overgrown.apoli.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record ParticleEffect(JsonElement raw) {

    public static final ParticleEffect EMPTY = new ParticleEffect(new JsonPrimitive("minecraft:poof"));

    public static final Codec<ParticleEffect> CODEC = Codec.PASSTHROUGH.xmap(
        dynamic -> new ParticleEffect(dynamic.convert(JsonOps.INSTANCE).getValue()),
        effect -> new Dynamic<>(JsonOps.INSTANCE, effect.raw)
    );

    public @Nullable ParticleOptions resolve(Level level) {
        RegistryOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);

        if (raw.isJsonPrimitive()) {
            JsonObject obj = new JsonObject();
            obj.add("type", raw);
            return ParticleTypes.CODEC.parse(ops, obj).result().orElse(null);
        }

        if (raw.isJsonObject()) {
            JsonObject obj = raw.getAsJsonObject();
            if (obj.has("params")) {
                String type = obj.has("type") ? obj.get("type").getAsString() : "";
                String params = obj.get("params").getAsString();
                String command = params.isEmpty()
                    ? type
                    : type + (params.startsWith("{") ? params : "{" + params + "}");
                try {
                    return ParticleArgument.readParticle(new StringReader(command), level.registryAccess());
                } catch (Exception ignored) {
                    return null;
                }
            }
            return ParticleTypes.CODEC.parse(ops, obj).result().orElse(null);
        }

        return null;
    }
}
