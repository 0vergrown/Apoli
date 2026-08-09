package dev.overgrown.apoli.global;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Optional;

public record GlobalPowerSet(
    ResourceLocation id,
    Optional<List<TypeMatcher>> entityTypes,
    List<ResourceLocation> powers,
    boolean replace,
    int order,
    int loadingPriority
) implements Comparable<GlobalPowerSet> {

    public GlobalPowerSet {
        powers = List.copyOf(powers);
        entityTypes = entityTypes.map(List::copyOf);
    }

    public record TypeMatcher(Optional<ResourceLocation> type, Optional<TagKey<EntityType<?>>> tag) {

        public static final Codec<TypeMatcher> CODEC = Codec.STRING.comapFlatMap(
            raw -> {
                boolean tagged = raw.startsWith("#");
                ResourceLocation parsed = ResourceLocation.tryParse(tagged ? raw.substring(1) : raw);
                if (parsed == null) {
                    return DataResult.error(() -> "Not a valid entity type or tag: " + raw);
                }
                return DataResult.success(tagged
                    ? new TypeMatcher(Optional.empty(), Optional.of(TagKey.create(Registries.ENTITY_TYPE, parsed)))
                    : new TypeMatcher(Optional.of(parsed), Optional.empty()));
            },
            matcher -> matcher.tag()
                .map(tag -> "#" + tag.location())
                .orElseGet(() -> matcher.type().orElseThrow().toString())
        );

        public boolean matches(EntityType<?> entityType) {
            if (tag.isPresent()) {
                return entityType.builtInRegistryHolder().is(tag.get());
            }
            return type.map(id -> id.equals(EntityType.getKey(entityType))).orElse(false);
        }
    }

    private static final Codec<List<TypeMatcher>> TYPES_CODEC = Codec.either(
        TypeMatcher.CODEC, TypeMatcher.CODEC.listOf()
    ).xmap(
        either -> either.map(List::of, list -> list),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
    );

    public static Codec<GlobalPowerSet> codec(ResourceLocation id) {
        return RecordCodecBuilder.create(instance -> instance.group(
            TYPES_CODEC.optionalFieldOf("entity_types").forGetter(GlobalPowerSet::entityTypes),
            ResourceLocation.CODEC.listOf().fieldOf("powers").forGetter(GlobalPowerSet::powers),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(GlobalPowerSet::replace),
            Codec.INT.optionalFieldOf("order", 0).forGetter(GlobalPowerSet::order),
            Codec.INT.optionalFieldOf("loading_priority", 0).forGetter(GlobalPowerSet::loadingPriority)
        ).apply(instance, (types, powers, replace, order, priority) ->
            new GlobalPowerSet(id, types, powers, replace, order, priority)));
    }

    public boolean appliesTo(EntityType<?> entityType) {
        if (entityTypes.isEmpty()) return true;
        List<TypeMatcher> matchers = entityTypes.get();
        for (int i = 0; i < matchers.size(); i++) {
            if (matchers.get(i).matches(entityType)) return true;
        }
        return false;
    }

    @Override
    public int compareTo(GlobalPowerSet other) {
        int byOrder = Integer.compare(order, other.order);
        return byOrder != 0 ? byOrder : id.compareTo(other.id);
    }
}
