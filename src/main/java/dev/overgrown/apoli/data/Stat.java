package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class Stat {
    private static final ResourceLocation CUSTOM = new ResourceLocation("custom");

    private final ResourceLocation type;
    private final ResourceLocation id;

    private @Nullable net.minecraft.stats.Stat<?> resolved;
    private boolean resolveAttempted;

    public Stat(ResourceLocation type, ResourceLocation id) {
        this.type = type;
        this.id = id;
    }

    public ResourceLocation type() {
        return type;
    }

    public ResourceLocation id() {
        return id;
    }

    private static final Codec<Stat> OBJECT_CODEC = RecordCodecBuilder.create(i -> i.group(
        IdCodecs.ID.fieldOf("type").forGetter(Stat::type),
        IdCodecs.ID.fieldOf("id").forGetter(Stat::id)
    ).apply(i, Stat::new));

    public static final Codec<Stat> CODEC = Codec.either(
        IdCodecs.ID.xmap(id -> new Stat(CUSTOM, id), Stat::id),
        OBJECT_CODEC
    ).xmap(
        either -> either.map(Function.identity(), Function.identity()),
        stat -> CUSTOM.equals(stat.type()) ? Either.left(stat) : Either.right(stat)
    );

    @SuppressWarnings({"rawtypes", "unchecked"})
    public @Nullable net.minecraft.stats.Stat<?> resolve() {
        if (!resolveAttempted) {
            resolveAttempted = true;
            StatType statType = BuiltInRegistries.STAT_TYPE.get(type);
            if (statType != null) {
                Registry<?> reg = statType.getRegistry();
                Object value = reg.get(id);
                if (value != null) {
                    resolved = statType.get(value);
                }
            }
        }
        return resolved;
    }

    public static void setValue(ServerPlayer player, net.minecraft.stats.Stat<?> stat, int value) {
        player.getStats().setValue(player, stat, value);
        player.level().getScoreboard().forAllObjectives(stat, player.getScoreboardName(), score -> score.setScore(value));
    }
}
