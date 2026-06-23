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

public class AddPowerFunction extends LootItemConditionalFunction {
    final ResourceLocation power;
    final List<EquipmentSlot> slots;
    final boolean hidden;
    final boolean negative;

    AddPowerFunction(LootItemCondition[] conditions, ResourceLocation power, List<EquipmentSlot> slots,
                     boolean hidden, boolean negative) {
        super(conditions);
        this.power = power;
        this.slots = slots;
        this.hidden = hidden;
        this.negative = negative;
    }

    @Override
    public LootItemFunctionType getType() {
        return ApoliLootFunctions.ADD_POWER;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        for (EquipmentSlot slot : slots) {
            ItemPowers.add(stack, power, slot, hidden, negative);
        }
        return stack;
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<AddPowerFunction> {
        @Override
        public void serialize(JsonObject json, AddPowerFunction fn, JsonSerializationContext ctx) {
            super.serialize(json, fn, ctx);
            json.addProperty("power", fn.power.toString());
            JsonArray slots = new JsonArray();
            for (EquipmentSlot s : fn.slots) slots.add(s.getSerializedName());
            json.add("slot", slots);
            if (fn.hidden) json.addProperty("hidden", true);
            if (fn.negative) json.addProperty("negative", true);
        }

        @Override
        public AddPowerFunction deserialize(JsonObject json, JsonDeserializationContext ctx, LootItemCondition[] conditions) {
            ResourceLocation power = new ResourceLocation(GsonHelper.getAsString(json, "power"));
            List<EquipmentSlot> slots = new ArrayList<>();
            for (JsonElement el : GsonHelper.getAsJsonArray(json, "slot")) {
                EquipmentSlot slot = ItemPowers.slotByName(el.getAsString());
                if (slot != null) slots.add(slot);
            }
            boolean hidden = GsonHelper.getAsBoolean(json, "hidden", false);
            boolean negative = GsonHelper.getAsBoolean(json, "negative", false);
            return new AddPowerFunction(conditions, power, slots, hidden, negative);
        }
    }
}
