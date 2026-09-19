package dev.overgrown.apoli.advancement;

import com.google.gson.JsonObject;
import dev.overgrown.apoli.Apoli;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public final class ResourceTrigger extends SimpleCriterionTrigger<ResourceTrigger.TriggerInstance> {

    public static final ResourceLocation ID = Apoli.id("resource");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player,
                                             DeserializationContext context) {
        ResourceLocation resource = json.has("resource")
            ? new ResourceLocation(GsonHelper.getAsString(json, "resource")) : null;
        return new TriggerInstance(player, resource, MinMaxBounds.Ints.fromJson(json.get("value")));
    }

    public void trigger(ServerPlayer player, ResourceLocation resource, int value) {
        this.trigger(player, instance -> instance.matches(resource, value));
    }

    public static final class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final @Nullable ResourceLocation resource;
        private final MinMaxBounds.Ints value;

        public TriggerInstance(ContextAwarePredicate player, @Nullable ResourceLocation resource,
                               MinMaxBounds.Ints value) {
            super(ID, player);
            this.resource = resource;
            this.value = value;
        }

        public boolean matches(ResourceLocation changed, int current) {
            if (resource != null && !resource.equals(changed)) return false;
            return value.matches(current);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            if (resource != null) json.addProperty("resource", resource.toString());
            json.add("value", value.serializeToJson());
            return json;
        }
    }
}
