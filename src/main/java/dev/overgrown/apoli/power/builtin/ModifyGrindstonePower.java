package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public final class ModifyGrindstonePower extends PowerType<ModifyGrindstonePower.Config> {
    public enum ResultType implements StringRepresentable {
        UNCHANGED("unchanged"),
        SPECIFIED("specified"),
        FROM_TOP("from_top"),
        FROM_BOTTOM("from_bottom");

        public static final Codec<ResultType> CODEC = StringRepresentable.fromEnum(ResultType::values);
        private final String name;
        ResultType(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public record Config(
        ResultType resultType,
        Optional<EntityAction> entityAction,
        Optional<BlockAction> blockAction,
        Optional<ItemAction> itemAction,
        Optional<ItemAction> itemActionAfterGrinding,
        Optional<ItemCondition> topCondition,
        Optional<ItemCondition> bottomCondition,
        Optional<ItemCondition> outputCondition,
        Optional<ItemStackData> resultStack,
        Optional<AttributeModifier> xpModifier
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResultType.CODEC.optionalFieldOf("result_type", ResultType.UNCHANGED).forGetter(Config::resultType),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("block_action", BlockAction.CODEC).forGetter(Config::blockAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Config::itemAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action_after_grinding", ItemAction.CODEC).forGetter(Config::itemActionAfterGrinding),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("top_condition", ItemCondition.CODEC).forGetter(Config::topCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bottom_condition", ItemCondition.CODEC).forGetter(Config::bottomCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("output_condition", ItemCondition.CODEC).forGetter(Config::outputCondition),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            AttributeModifier.CODEC.optionalFieldOf("xp_modifier").forGetter(Config::xpModifier)
        ).apply(i, Config::new));
    }
}
