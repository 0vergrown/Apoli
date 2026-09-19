package dev.overgrown.apoli.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class ResourceTrigger extends SimpleCriterionTrigger<ResourceTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceLocation resource, int value) {
        this.trigger(player, instance -> instance.matches(resource, value));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<ResourceLocation> resource,
                                  MinMaxBounds.Ints value) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            IdCodecs.ID.optionalFieldOf("resource").forGetter(TriggerInstance::resource),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("value", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::value)
        ).apply(i, TriggerInstance::new));

        public boolean matches(ResourceLocation changed, int current) {
            if (resource.isPresent() && !resource.get().equals(changed)) return false;
            return value.matches(current);
        }
    }
}
