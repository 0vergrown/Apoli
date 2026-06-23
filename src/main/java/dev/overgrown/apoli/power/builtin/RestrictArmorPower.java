package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Optional;

public final class RestrictArmorPower extends PowerType<RestrictArmorPower.Config> {
    public record Config(
        Optional<ItemCondition> head,
        Optional<ItemCondition> chest,
        Optional<ItemCondition> legs,
        Optional<ItemCondition> feet
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ItemCondition.CODEC.optionalFieldOf("head").forGetter(Config::head),
            ItemCondition.CODEC.optionalFieldOf("chest").forGetter(Config::chest),
            ItemCondition.CODEC.optionalFieldOf("legs").forGetter(Config::legs),
            ItemCondition.CODEC.optionalFieldOf("feet").forGetter(Config::feet)
        ).apply(i, Config::new));
    }

    public static boolean blocks(Config cfg, EquipmentSlot slot,
                                 dev.overgrown.apoli.condition.context.ItemCtx ctx) {
        Optional<ItemCondition> rule = switch (slot) {
            case HEAD -> cfg.head;
            case CHEST -> cfg.chest;
            case LEGS -> cfg.legs;
            case FEET -> cfg.feet;
            default -> Optional.empty();
        };
        return rule.isPresent() && rule.get().test(ctx);
    }
}
