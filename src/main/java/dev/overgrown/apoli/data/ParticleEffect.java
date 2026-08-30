package dev.overgrown.apoli.data;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ParticleEffect {

    public static final ParticleEffect EMPTY =
        new ParticleEffect(new Dynamic<>(NbtOps.INSTANCE).createString("minecraft:poof"));

    public static final Codec<ParticleEffect> CODEC = Codec.PASSTHROUGH.xmap(
        ParticleEffect::new,
        ParticleEffect::raw
    );

    private record Resolved(RegistryAccess registries, @Nullable ParticleOptions options) {}

    private final Dynamic<?> raw;
    private volatile @Nullable Resolved resolved;

    public ParticleEffect(Dynamic<?> raw) {
        this.raw = raw;
    }

    public Dynamic<?> raw() {
        return this.raw;
    }

    public @Nullable ParticleOptions resolve(Level level) {
        RegistryAccess registries = level.registryAccess();
        Resolved hit = this.resolved;
        if (hit != null && hit.registries() == registries) return hit.options();
        ParticleOptions built = parse(level, registries);
        this.resolved = new Resolved(registries, built);
        return built;
    }

    private @Nullable ParticleOptions parse(Level level, RegistryAccess registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        Dynamic<Tag> data = raw.convert(NbtOps.INSTANCE);

        String simple = data.asString().result().orElse(null);
        if (simple != null) {
            Dynamic<Tag> wrapped = data.emptyMap().set("type", data.createString(simple));
            return ParticleTypes.CODEC.parse(ops, wrapped.getValue()).result().orElse(null);
        }

        if (data.getMapValues().result().isEmpty()) return null;

        String params = data.get("params").asString().result().orElse(null);
        if (params != null) {
            String type = data.get("type").asString().result().orElse("");
            String command = params.isEmpty()
                ? type
                : type + (params.startsWith("{") ? params : "{" + params + "}");
            try {
                return ParticleArgument.readParticle(new StringReader(command), registries);
            } catch (Exception ignored) {
                return null;
            }
        }

        return ParticleTypes.CODEC.parse(ops, data.getValue()).result().orElse(null);
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
