package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class ModifyCraftingPower extends PowerType<ModifyCraftingPower.Config> {
    public record Config(
        Optional<ResourceLocation> recipe,
        Optional<ItemAction> itemAction,
        Optional<ItemAction> itemActionAfterCrafting,
        Optional<EntityAction> entityAction,
        Optional<BlockAction> blockAction,
        Optional<ItemCondition> itemCondition,
        Optional<ItemStackData> result
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("recipe").forGetter(Config::recipe),
            ItemAction.CODEC.optionalFieldOf("item_action").forGetter(Config::itemAction),
            ItemAction.CODEC.optionalFieldOf("item_action_after_crafting").forGetter(Config::itemActionAfterCrafting),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            BlockAction.CODEC.optionalFieldOf("block_action").forGetter(Config::blockAction),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            ItemStackData.CODEC.optionalFieldOf("result").forGetter(Config::result)
        ).apply(i, Config::new));
    }
}
