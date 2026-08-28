package dev.overgrown.apoli.data;

import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ParticleEffect(Dynamic<?> raw) {

    public static final ParticleEffect EMPTY =
        new ParticleEffect(new Dynamic<>(JsonOps.INSTANCE).createString("minecraft:poof"));

    public static final Codec<ParticleEffect> CODEC = Codec.PASSTHROUGH.xmap(
        ParticleEffect::new,
        ParticleEffect::raw
    );

    public @Nullable ParticleOptions resolve(net.minecraft.world.level.Level level) {
        Dynamic<JsonElement> data = raw.convert(JsonOps.INSTANCE);

        String simple = data.asString().result().orElse(null);
        if (simple != null) return fromCommand(simple, "");

        if (data.getMapValues().result().isEmpty()) return null;

        String params = data.get("params").asString().result().orElse(null);
        if (params != null) return fromCommand(data.get("type").asString().result().orElse(""), params);

        return ParticleTypes.CODEC.parse(data).result().orElse(null);
    }

    private static @Nullable ParticleOptions fromCommand(String rawType, String params) {
        ResourceLocation id = ResourceLocation.tryParse(rawType);
        if (id == null) return null;
        ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(id);
        return particleType == null ? null : readUnchecked(particleType, params);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable ParticleOptions readUnchecked(ParticleType<?> rawType, String params) {
        try {
            ParticleType type = rawType;
            String command = params.isEmpty() || Character.isWhitespace(params.charAt(0)) ? params : " " + params;
            StringReader reader = new StringReader(command);
            return type.getDeserializer().fromCommand(type, reader);
        } catch (Exception e) {
            return null;
        }
    }
}
