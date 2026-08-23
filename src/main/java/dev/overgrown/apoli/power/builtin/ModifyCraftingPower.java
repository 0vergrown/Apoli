package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
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
            IdCodecs.ID.optionalFieldOf("recipe").forGetter(Config::recipe),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Config::itemAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action_after_crafting", ItemAction.CODEC).forGetter(Config::itemActionAfterCrafting),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("block_action", BlockAction.CODEC).forGetter(Config::blockAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            ItemStackData.CODEC.optionalFieldOf("result").forGetter(Config::result)
        ).apply(i, Config::new));
    }
}
