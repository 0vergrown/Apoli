package dev.overgrown.apoli.item;

import com.mojang.serialization.Codec;
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

public class AddPowerFunction extends LootItemConditionalFunction {
    public static final MapCodec<AddPowerFunction> CODEC = RecordCodecBuilder.mapCodec(
        instance -> commonFields(instance).and(instance.group(
            ResourceLocation.CODEC.fieldOf("power").forGetter(f -> f.power),
            EquipmentSlot.CODEC.listOf().fieldOf("slot").forGetter(f -> f.slots),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(f -> f.hidden),
            Codec.BOOL.optionalFieldOf("negative", false).forGetter(f -> f.negative)
        )).apply(instance, AddPowerFunction::new));

    private final ResourceLocation power;
    private final List<EquipmentSlot> slots;
    private final boolean hidden;
    private final boolean negative;

    protected AddPowerFunction(List<LootItemCondition> conditions, ResourceLocation power,
                               List<EquipmentSlot> slots, boolean hidden, boolean negative) {
        super(conditions);
        this.power = power;
        this.slots = slots;
        this.hidden = hidden;
        this.negative = negative;
    }

    @Override
    public LootItemFunctionType<AddPowerFunction> getType() {
        return ApoliLootFunctions.ADD_POWER;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        for (EquipmentSlot slot : slots) {
            ItemPowers.add(stack, power, slot, hidden, negative);
        }
        return stack;
    }
}
