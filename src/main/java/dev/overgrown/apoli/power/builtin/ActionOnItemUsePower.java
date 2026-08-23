package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public final class ActionOnItemUsePower extends PowerType<ActionOnItemUsePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("action_on_item_use");

    public record Config(
        Optional<EntityAction> entityAction,
        Optional<ItemAction> itemAction,
        Optional<ItemCondition> itemCondition,
        Trigger trigger,
        int priority
    ) {}

    public enum Trigger implements StringRepresentable {
        START("start"),
        DURING("during"),
        STOP("stop"),
        FINISH("finish"),
        INSTANT("instant");

        public static final Codec<Trigger> CODEC = StringRepresentable.fromEnum(Trigger::values);
        private final String name;
        Trigger(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Config::itemAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            Trigger.CODEC.optionalFieldOf("trigger", Trigger.FINISH).forGetter(Config::trigger),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }
}
