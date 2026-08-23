package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.FoodComponent;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.UseAnim;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EdibleItemPower extends PowerType<EdibleItemPower.Config> {
    public enum ConsumeAnimation implements StringRepresentable {
        EAT("eat", UseAnim.EAT),
        DRINK("drink", UseAnim.DRINK);

        public static final Codec<ConsumeAnimation> CODEC = StringRepresentable.fromEnum(ConsumeAnimation::values);
        private final String name;
        private final UseAnim vanilla;
        ConsumeAnimation(String n, UseAnim a) {
            this.name = n; this.vanilla = a;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public UseAnim vanilla() {
            return vanilla;
        }
    }

    public record Config(
        Optional<EntityAction> entityAction,
        Optional<ItemAction> itemAction,
        Optional<ItemAction> resultItemAction,
        Optional<ItemCondition> itemCondition,
        FoodComponent foodComponent,
        Optional<ItemStackData> resultStack,
        ConsumeAnimation consumeAnimation,
        ResourceLocation consumeSound,
        Optional<AttributeModifier> consumingTimeModifier,
        Optional<List<AttributeModifier>> consumingTimeModifiers
    ) {}

    private static final ResourceLocation DEFAULT_EAT_SOUND = ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.eat");

    private static final MapCodec<Config> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Config::itemAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("result_item_action", ItemAction.CODEC).forGetter(Config::resultItemAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
        FoodComponent.CODEC.fieldOf("food_component").forGetter(Config::foodComponent),
        ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
        ConsumeAnimation.CODEC.optionalFieldOf("consume_animation", ConsumeAnimation.EAT).forGetter(Config::consumeAnimation),
        IdCodecs.ID.optionalFieldOf("consume_sound", DEFAULT_EAT_SOUND).forGetter(Config::consumeSound),
        AttributeModifier.CODEC.optionalFieldOf("consuming_time_modifier").forGetter(Config::consumingTimeModifier),
        Codec.list(AttributeModifier.CODEC).optionalFieldOf("consuming_time_modifiers").forGetter(Config::consumingTimeModifiers)
    ).apply(i, Config::new));

    private static final MapCodec<Config> ALIASED = AliasingMapCodec.wrap(INNER, Map.of(
        "use_action", "consume_animation",
        "sound", "consume_sound",
        "return_stack", "result_stack"
    ));

    @Override
    public MapCodec<Config> configCodec() {
        return ALIASED;
    }
}
