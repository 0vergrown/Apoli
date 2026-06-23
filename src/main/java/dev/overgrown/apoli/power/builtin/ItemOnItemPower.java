package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public final class ItemOnItemPower extends PowerType<ItemOnItemPower.Config> {
    public enum ClickType implements StringRepresentable {
        PRIMARY("primary", 0),
        SECONDARY("secondary", 1);

        public static final Codec<ClickType> NAME_CODEC = StringRepresentable.fromEnum(ClickType::values);
        public static final Codec<ClickType> CODEC = Codec.either(NAME_CODEC, Codec.INT).xmap(
            e -> e.map(java.util.function.Function.identity(), ClickType::fromInt),
            t -> Either.left(t)
        );

        private final String name;
        private final int idx;

        ClickType(String name, int idx) {
            this.name = name; this.idx = idx;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public int index() {
            return idx;
        }

        public static ClickType fromInt(int i) {
            return i == 0 ? PRIMARY : SECONDARY;
        }
    }

    public record Config(
        Optional<ItemCondition> usingItemCondition,
        Optional<ItemCondition> onItemCondition,
        Optional<ItemStackData> result,
        int resultFromOnStack,
        Optional<ItemAction> usingItemAction,
        Optional<ItemAction> onItemAction,
        Optional<ItemAction> resultItemAction,
        Optional<EntityAction> entityAction,
        ClickType clickType
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ItemCondition.CODEC.optionalFieldOf("using_item_condition").forGetter(Config::usingItemCondition),
            ItemCondition.CODEC.optionalFieldOf("on_item_condition").forGetter(Config::onItemCondition),
            ItemStackData.CODEC.optionalFieldOf("result").forGetter(Config::result),
            Codec.INT.optionalFieldOf("result_from_on_stack", 0).forGetter(Config::resultFromOnStack),
            ItemAction.CODEC.optionalFieldOf("using_item_action").forGetter(Config::usingItemAction),
            ItemAction.CODEC.optionalFieldOf("on_item_action").forGetter(Config::onItemAction),
            ItemAction.CODEC.optionalFieldOf("result_item_action").forGetter(Config::resultItemAction),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            ClickType.CODEC.optionalFieldOf("click_type", ClickType.SECONDARY).forGetter(Config::clickType)
        ).apply(i, Config::new));
    }
}
