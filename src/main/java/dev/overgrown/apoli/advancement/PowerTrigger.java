package dev.overgrown.apoli.advancement;

import com.google.gson.JsonObject;
import dev.overgrown.apoli.Apoli;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public final class PowerTrigger extends SimpleCriterionTrigger<PowerTrigger.TriggerInstance> {

    public static final ResourceLocation ID = Apoli.id("power");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player,
                                             DeserializationContext context) {
        ResourceLocation power = json.has("power")
            ? new ResourceLocation(GsonHelper.getAsString(json, "power")) : null;
        ResourceLocation source = json.has("source")
            ? new ResourceLocation(GsonHelper.getAsString(json, "source")) : null;
        return new TriggerInstance(player, power, source);
    }

    public void trigger(ServerPlayer player, ResourceLocation power, ResourceLocation source) {
        this.trigger(player, instance -> instance.matches(power, source));
    }

    public static final class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final @Nullable ResourceLocation power;
        private final @Nullable ResourceLocation source;

        public TriggerInstance(ContextAwarePredicate player, @Nullable ResourceLocation power,
                               @Nullable ResourceLocation source) {
            super(ID, player);
            this.power = power;
            this.source = source;
        }

        public boolean matches(ResourceLocation granted, ResourceLocation grantedFrom) {
            if (power != null && !power.equals(granted)) return false;
            return source == null || source.equals(grantedFrom);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            if (power != null) json.addProperty("power", power.toString());
            if (source != null) json.addProperty("source", source.toString());
            return json;
        }
    }
}
