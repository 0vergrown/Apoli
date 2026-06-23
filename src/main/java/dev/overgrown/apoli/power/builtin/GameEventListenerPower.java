package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Optional;

public final class GameEventListenerPower extends PowerType<GameEventListenerPower.Config> {
    public enum TriggerOrder implements StringRepresentable {
        BY_DISTANCE("by_distance"),
        UNSPECIFIED("unspecified");

        public static final Codec<TriggerOrder> CODEC = StringRepresentable.fromEnum(TriggerOrder::values);
        private final String name;
        TriggerOrder(String n) {
            this.name = n;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Config(
        TriggerOrder triggerOrder,
        boolean entity,
        boolean block,
        Optional<BiEntityAction> bientityAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<BlockAction> blockAction,
        Optional<BlockCondition> blockCondition,
        int cooldown,
        HudRender hudRender,
        Optional<ResourceLocation> event,
        Optional<List<ResourceLocation>> events,
        Optional<ResourceLocation> eventTag,
        boolean showParticle
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TriggerOrder.CODEC.optionalFieldOf("trigger_order", TriggerOrder.UNSPECIFIED).forGetter(Config::triggerOrder),
            Codec.BOOL.optionalFieldOf("entity", true).forGetter(Config::entity),
            Codec.BOOL.optionalFieldOf("block", true).forGetter(Config::block),
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Config::bientityAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            BlockAction.CODEC.optionalFieldOf("block_action").forGetter(Config::blockAction),
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            Codec.INT.optionalFieldOf("cooldown", 1).forGetter(Config::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender),
            ResourceLocation.CODEC.optionalFieldOf("event").forGetter(Config::event),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("events").forGetter(Config::events),
            ResourceLocation.CODEC.optionalFieldOf("event_tag").forGetter(Config::eventTag),
            Codec.BOOL.optionalFieldOf("show_particle", true).forGetter(Config::showParticle)
        ).apply(i, Config::new));
    }
}
