package dev.overgrown.apoli.global;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.data.IdOrTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Optional;

public record GlobalPowerSet(
    ResourceLocation id,
    Optional<List<IdOrTag<EntityType<?>>>> entityTypes,
    List<ResourceLocation> powers,
    boolean replace,
    int order,
    int loadingPriority
) implements Comparable<GlobalPowerSet> {

    public GlobalPowerSet {
        powers = List.copyOf(powers);
        entityTypes = entityTypes.map(List::copyOf);
    }

    private static final Codec<IdOrTag<EntityType<?>>> TYPE_CODEC =
        IdOrTag.codec(Registries.ENTITY_TYPE);

    private static final Codec<List<IdOrTag<EntityType<?>>>> TYPES_CODEC = Codec.either(
        TYPE_CODEC, TYPE_CODEC.listOf()
    ).xmap(
        either -> either.map(List::of, list -> list),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
    );

    public static Codec<GlobalPowerSet> codec(ResourceLocation id) {
        return RecordCodecBuilder.create(instance -> instance.group(
            TYPES_CODEC.optionalFieldOf("entity_types").forGetter(GlobalPowerSet::entityTypes),
            IdCodecs.ID.listOf().fieldOf("powers").forGetter(GlobalPowerSet::powers),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(GlobalPowerSet::replace),
            Codec.INT.optionalFieldOf("order", 0).forGetter(GlobalPowerSet::order),
            Codec.INT.optionalFieldOf("loading_priority", 0).forGetter(GlobalPowerSet::loadingPriority)
        ).apply(instance, (types, powers, replace, order, priority) ->
            new GlobalPowerSet(id, types, powers, replace, order, priority)));
    }

    public boolean appliesTo(EntityType<?> entityType) {
        if (entityTypes.isEmpty()) return true;
        List<IdOrTag<EntityType<?>>> matchers = entityTypes.get();
        for (int i = 0; i < matchers.size(); i++) {
            if (matchers.get(i).matches(entityType.builtInRegistryHolder())) return true;
        }
        return false;
    }

    @Override
    public int compareTo(GlobalPowerSet other) {
        int byOrder = Integer.compare(order, other.order);
        return byOrder != 0 ? byOrder : id.compareTo(other.id);
    }
}
