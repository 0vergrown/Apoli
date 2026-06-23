package dev.overgrown.apoli.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class RemovePowerFunction extends LootItemConditionalFunction {
    public static final MapCodec<RemovePowerFunction> CODEC = RecordCodecBuilder.mapCodec(
        instance -> commonFields(instance).and(instance.group(
            ResourceLocation.CODEC.fieldOf("power").forGetter(f -> f.power),
            EquipmentSlot.CODEC.listOf().optionalFieldOf("slot", List.of()).forGetter(f -> f.slots)
        )).apply(instance, RemovePowerFunction::new));

    private final ResourceLocation power;
    private final List<EquipmentSlot> slots;

    protected RemovePowerFunction(List<LootItemCondition> conditions, ResourceLocation power, List<EquipmentSlot> slots) {
        super(conditions);
        this.power = power;
        this.slots = slots;
    }

    @Override
    public LootItemFunctionType<RemovePowerFunction> getType() {
        return ApoliLootFunctions.REMOVE_POWER;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (slots.isEmpty()) {
            ItemPowers.remove(stack, power, null);
        } else {
            for (EquipmentSlot slot : slots) {
                ItemPowers.remove(stack, power, slot);
            }
        }
        return stack;
    }
}
