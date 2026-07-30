package dev.overgrown.apoli.compat.voicechat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.message.MessageFilter;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ActionOnSendingMessagePower extends PowerType<ActionOnSendingMessagePower.Config> {
    public record Config(
        Optional<ResourceLocation> messageType,
        Optional<EntityAction> entityAction,
        List<MessageFilter> filters,
        int priority
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("message_type").forGetter(Config::messageType),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            MessageFilter.CODEC.optionalFieldOf("filter").forGetter(c -> Optional.<MessageFilter>empty()),
            MessageFilter.CODEC.listOf().optionalFieldOf("filters", List.of()).forGetter(Config::filters),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, (messageType, entityAction, filter, filters, priority) -> {
            List<MessageFilter> merged = new ArrayList<>(filters);
            filter.ifPresent(merged::add);
            return new Config(messageType, entityAction, merged, priority);
        }));
    }

    public static boolean process(Config config, Entity entity, String text, ResourceLocation typeId) {
        if (config.messageType().isPresent() && !config.messageType().get().equals(typeId)) {
            return false;
        }
        EntityCtx ctx = new EntityCtx(entity, entity.level());
        if (config.filters().isEmpty()) {
            config.entityAction().ifPresent(action -> action.run(ctx));
            return false;
        }
        boolean prevented = false;
        for (MessageFilter filter : config.filters()) {
            if (!filter.matches(text)) {
                continue;
            }
            filter.beforeAction().ifPresent(action -> action.run(ctx));
            config.entityAction().ifPresent(action -> action.run(ctx));
            filter.afterAction().ifPresent(action -> action.run(ctx));
            if (filter.prevent()) {
                prevented = true;
                break;
            }
        }
        return prevented;
    }
}
