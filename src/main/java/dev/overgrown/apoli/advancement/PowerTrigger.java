package dev.overgrown.apoli.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class PowerTrigger extends SimpleCriterionTrigger<PowerTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceLocation power, ResourceLocation source) {
        this.trigger(player, instance -> instance.matches(power, source));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<ResourceLocation> power,
                                  Optional<ResourceLocation> source) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            IdCodecs.ID.optionalFieldOf("power").forGetter(TriggerInstance::power),
            IdCodecs.ID.optionalFieldOf("source").forGetter(TriggerInstance::source)
        ).apply(i, TriggerInstance::new));

        public boolean matches(ResourceLocation granted, ResourceLocation grantedFrom) {
            if (power.isPresent() && !power.get().equals(granted)) return false;
            return source.isEmpty() || source.get().equals(grantedFrom);
        }
    }
}
