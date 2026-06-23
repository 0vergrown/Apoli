package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.Hand;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import com.mojang.serialization.Codec;

import java.util.List;
import java.util.Optional;

public final class PreventBlockPlacePower extends PowerType<PreventBlockPlacePower.Config> {
    public record Config(
        Optional<EntityAction> entityAction,
        Optional<ItemAction> heldItemAction,
        Optional<BlockAction> placeToAction,
        Optional<BlockAction> placeOnAction,
        Optional<ItemCondition> itemCondition,
        Optional<BlockCondition> placeToCondition,
        Optional<BlockCondition> placeOnCondition,
        List<Dir> directions,
        List<Hand> hands,
        Optional<ItemStackData> resultStack,
        Optional<ItemAction> resultItemAction
    ) {}

    public enum Dir implements StringRepresentable {
        UP("up", Direction.UP), DOWN("down", Direction.DOWN),
        NORTH("north", Direction.NORTH), SOUTH("south", Direction.SOUTH),
        EAST("east", Direction.EAST), WEST("west", Direction.WEST);

        public static final Codec<Dir> CODEC = StringRepresentable.fromEnum(Dir::values);
        public static final Codec<List<Dir>> LIST_CODEC = Codec.list(CODEC);
        public static final List<Dir> ALL = List.of(values());

        private final String name;
        private final Direction vanilla;
        Dir(String n, Direction v) { this.name = n; this.vanilla = v; }
        public Direction vanilla() { return vanilla; }
        @Override public String getSerializedName() { return name; }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            ItemAction.CODEC.optionalFieldOf("held_item_action").forGetter(Config::heldItemAction),
            BlockAction.CODEC.optionalFieldOf("place_to_action").forGetter(Config::placeToAction),
            BlockAction.CODEC.optionalFieldOf("place_on_action").forGetter(Config::placeOnAction),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            BlockCondition.CODEC.optionalFieldOf("place_to_condition").forGetter(Config::placeToCondition),
            BlockCondition.CODEC.optionalFieldOf("place_on_condition").forGetter(Config::placeOnCondition),
            Dir.LIST_CODEC.optionalFieldOf("directions", Dir.ALL).forGetter(Config::directions),
            Hand.LIST_CODEC.optionalFieldOf("hands", Hand.BOTH).forGetter(Config::hands),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            ItemAction.CODEC.optionalFieldOf("result_item_action").forGetter(Config::resultItemAction)
        ).apply(i, Config::new));
    }
}
