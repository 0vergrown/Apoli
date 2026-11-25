package dev.overgrown.apoli.compat.gun;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.data.AttributeModifier;

import java.util.List;
import java.util.Optional;

public final class GunConfigs {
    private GunConfigs() {}

    public record Damage(GunFilter filter, Optional<Boolean> headshot, Optional<AttributeModifier> modifier,
                         Optional<List<AttributeModifier>> modifiers, Optional<BiEntityCondition> bientityCondition) {
        public static final MapCodec<Damage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GunFilter.MAP_CODEC.forGetter(Damage::filter),
            LoggedOptionalField.strict("headshot", Codec.BOOL).forGetter(Damage::headshot),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Damage::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Damage::modifiers),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Damage::bientityCondition)
        ).apply(i, Damage::new));
    }

    public record Headshot(GunFilter filter, Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers,
                           Optional<BiEntityCondition> bientityCondition) {
        public static final MapCodec<Headshot> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GunFilter.MAP_CODEC.forGetter(Headshot::filter),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Headshot::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Headshot::modifiers),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Headshot::bientityCondition)
        ).apply(i, Headshot::new));
    }

    public record Stat(GunFilter filter, Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers) {
        public static final MapCodec<Stat> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GunFilter.MAP_CODEC.forGetter(Stat::filter),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Stat::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Stat::modifiers)
        ).apply(i, Stat::new));
    }

    public record Fire(GunFilter filter, Optional<EntityAction> entityAction, Optional<ItemAction> itemAction) {
        public static final MapCodec<Fire> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GunFilter.MAP_CODEC.forGetter(Fire::filter),
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Fire::entityAction),
            LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Fire::itemAction)
        ).apply(i, Fire::new));
    }

    public record BlockHit(GunFilter filter, Optional<BlockAction> blockAction, Optional<EntityAction> entityAction,
                           Optional<BlockCondition> blockCondition) {
        public static final MapCodec<BlockHit> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GunFilter.MAP_CODEC.forGetter(BlockHit::filter),
            LoggedOptionalField.of("block_action", BlockAction.CODEC).forGetter(BlockHit::blockAction),
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(BlockHit::entityAction),
            LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(BlockHit::blockCondition)
        ).apply(i, BlockHit::new));
    }
}
