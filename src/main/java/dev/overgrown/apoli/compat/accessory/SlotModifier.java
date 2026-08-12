package dev.overgrown.apoli.compat.accessory;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.AttributeModifierOperation;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;

public record SlotModifier(String slot, ResourceLocation id, double amount, AttributeModifierOperation operation) {

    public static final Codec<SlotModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("slot").forGetter(SlotModifier::slot),
        IdCodecs.ID.fieldOf("id").forGetter(SlotModifier::id),
        Codec.DOUBLE.optionalFieldOf("amount", 0.0).forGetter(SlotModifier::amount),
        AttributeModifierOperation.CODEC.optionalFieldOf("operation", AttributeModifierOperation.ADD_BASE_EARLY).forGetter(SlotModifier::operation)
    ).apply(i, SlotModifier::new));

    public static final Codec<List<SlotModifier>> LIST = Codec.either(CODEC, CODEC.listOf()).xmap(
        e -> e.map(List::of, l -> l),
        l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
    );

    public AttributeModifier toVanilla() {
        return new AttributeModifier(
            java.util.UUID.nameUUIDFromBytes(id.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            id.toString(),
            amount,
            operation.vanillaOperation()
        );
    }
}
