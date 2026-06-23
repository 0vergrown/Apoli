package dev.overgrown.apoli.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;

public class RemovePowerFunction extends LootItemConditionalFunction {
    final ResourceLocation power;
    final List<EquipmentSlot> slots;

    RemovePowerFunction(LootItemCondition[] conditions, ResourceLocation power, List<EquipmentSlot> slots) {
        super(conditions);
        this.power = power;
        this.slots = slots;
    }

    @Override
    public LootItemFunctionType getType() {
        return ApoliLootFunctions.REMOVE_POWER;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (slots.isEmpty()) {
            ItemPowers.remove(stack, power, null);
        } else {
            for (EquipmentSlot slot : slots) {
                ItemPowers.remove(stack, power, slot);
            }
        }
        return stack;
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<RemovePowerFunction> {
        @Override
        public void serialize(JsonObject json, RemovePowerFunction fn, JsonSerializationContext ctx) {
            super.serialize(json, fn, ctx);
            json.addProperty("power", fn.power.toString());
            if (!fn.slots.isEmpty()) {
                JsonArray slots = new JsonArray();
                for (EquipmentSlot s : fn.slots) slots.add(s.getSerializedName());
                json.add("slot", slots);
            }
        }

        @Override
        public RemovePowerFunction deserialize(JsonObject json, JsonDeserializationContext ctx, LootItemCondition[] conditions) {
            ResourceLocation power = new ResourceLocation(GsonHelper.getAsString(json, "power"));
            List<EquipmentSlot> slots = new ArrayList<>();
            if (json.has("slot")) {
                for (JsonElement el : GsonHelper.getAsJsonArray(json, "slot")) {
                    EquipmentSlot slot = ItemPowers.slotByName(el.getAsString());
                    if (slot != null) slots.add(slot);
                }
            }
            return new RemovePowerFunction(conditions, power, slots);
        }
    }
}
