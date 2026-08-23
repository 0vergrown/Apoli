package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ReceiveActionPower extends PowerType<ReceiveActionPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("receive_action");

    public record Config(
        Optional<EntityAction> action,
        Optional<BiEntityAction> bientityAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<EntityAction> entityAction,
        Optional<EntityCondition> entityCondition,
        Optional<ItemAction> itemAction,
        Optional<ItemCondition> itemCondition
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.of("action", EntityAction.CODEC).forGetter(Config::action),
            LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityAction),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Config::entityCondition),
            LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Config::itemAction),
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition)
        ).apply(i, Config::new));
    }

    @Nullable
    public static Config resolve(@Nullable Entity receiverHolder, ResourceLocation receiver) {
        if (receiverHolder == null) return null;
        PowerContainer container = PowerContainer.of(receiverHolder);
        if (container == null || !container.hasPower(receiver) || container.isSuppressed(receiver)) return null;
        Power loaded = ApoliPowers.get(receiver);
        if (loaded == null || !(loaded.config() instanceof Config cfg)) return null;
        if (!(PowerTypeRegistry.get(loaded.typeId()) instanceof ReceiveActionPower)) return null;
        if (loaded.condition().isPresent()
            && !loaded.condition().get().test(EntityCtx.of(receiverHolder, receiverHolder.level()))) {
            return null;
        }
        return cfg;
    }

    public static void receiveEntity(Entity holder, ResourceLocation receiver, EntityCtx ctx) {
        Config cfg = resolve(holder, receiver);
        if (cfg == null) return;
        if (cfg.entityCondition.isPresent() && !cfg.entityCondition.get().test(ctx)) return;
        cfg.entityAction.ifPresent(a -> a.run(ctx));
        cfg.action.ifPresent(a -> a.run(EntityCtx.of(holder, holder.level())));
    }

    public static void receiveBiEntity(Entity holder, ResourceLocation receiver, BiEntityCtx ctx) {
        Config cfg = resolve(holder, receiver);
        if (cfg == null) return;
        if (cfg.bientityCondition.isPresent() && !cfg.bientityCondition.get().test(ctx)) return;
        cfg.bientityAction.ifPresent(a -> a.run(ctx));
        cfg.action.ifPresent(a -> a.run(EntityCtx.of(holder, holder.level())));
    }

    public static void receiveItem(Entity holder, ResourceLocation receiver, ItemCtx ctx) {
        Config cfg = resolve(holder, receiver);
        if (cfg == null) return;
        if (cfg.itemCondition.isPresent() && !cfg.itemCondition.get().test(ctx)) return;
        cfg.itemAction.ifPresent(a -> a.run(ctx));
        cfg.action.ifPresent(a -> a.run(EntityCtx.of(holder, holder.level())));
    }
}
