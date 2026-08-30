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

public final class ParticleEffect {

    public static final ParticleEffect EMPTY =
        new ParticleEffect(new Dynamic<>(JsonOps.INSTANCE).createString("minecraft:poof"));

    public static final Codec<ParticleEffect> CODEC = Codec.PASSTHROUGH.xmap(
        ParticleEffect::new,
        ParticleEffect::raw
    );

    private record Resolved(@Nullable ParticleOptions options) {}

    private final Dynamic<?> raw;
    private volatile @Nullable Resolved resolved;

    public ParticleEffect(Dynamic<?> raw) {
        this.raw = raw;
    }

    public Dynamic<?> raw() {
        return this.raw;
    }

    public @Nullable ParticleOptions resolve(net.minecraft.world.level.Level level) {
        Resolved hit = this.resolved;
        if (hit != null) return hit.options();
        ParticleOptions built = parse();
        this.resolved = new Resolved(built);
        return built;
    }

    private @Nullable ParticleOptions parse() {
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

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof ParticleEffect effect && this.raw.equals(effect.raw));
    }

    @Override
    public int hashCode() {
        return this.raw.hashCode();
    }

    @Override
    public String toString() {
        return "ParticleEffect[" + this.raw + "]";
    }
}
