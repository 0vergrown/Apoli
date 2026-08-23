package dev.overgrown.apoli.data;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record ParticleEffect(Dynamic<?> raw) {

    public static final ParticleEffect EMPTY =
        new ParticleEffect(new Dynamic<>(NbtOps.INSTANCE).createString("minecraft:poof"));

    public static final Codec<ParticleEffect> CODEC = Codec.PASSTHROUGH.xmap(
        ParticleEffect::new,
        ParticleEffect::raw
    );

    public @Nullable ParticleOptions resolve(Level level) {
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
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
                return ParticleArgument.readParticle(new StringReader(command), level.registryAccess());
            } catch (Exception ignored) {
                return null;
            }
        }

        return ParticleTypes.CODEC.parse(ops, data.getValue()).result().orElse(null);
    }
}
