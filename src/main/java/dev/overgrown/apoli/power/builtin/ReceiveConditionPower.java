package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
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

public final class ReceiveConditionPower extends PowerType<ReceiveConditionPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("receive_condition");

    public record Config(
        Optional<BiEntityCondition> bientityCondition,
        Optional<EntityCondition> entityCondition,
        Optional<ItemCondition> itemCondition
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Config::entityCondition),
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition)
        ).apply(i, Config::new));
    }

    @Nullable
    private static Config resolve(@Nullable Entity holder, ResourceLocation receiver) {
        if (holder == null) return null;
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || !container.hasPower(receiver) || container.isSuppressed(receiver)) return null;
        Power loaded = ApoliPowers.get(receiver);
        if (loaded == null || !(loaded.config() instanceof Config cfg)) return null;
        if (!(PowerTypeRegistry.get(loaded.typeId()) instanceof ReceiveConditionPower)) return null;
        if (loaded.condition().isPresent()
            && !loaded.condition().get().test(EntityCtx.of(holder, holder.level()))) {
            return null;
        }
        return cfg;
    }

    public static boolean testEntity(Entity holder, ResourceLocation receiver, EntityCtx ctx) {
        Config cfg = resolve(holder, receiver);
        return cfg != null && (cfg.entityCondition.isEmpty() || cfg.entityCondition.get().test(ctx));
    }

    public static boolean testBiEntity(Entity holder, ResourceLocation receiver, BiEntityCtx ctx) {
        Config cfg = resolve(holder, receiver);
        return cfg != null && (cfg.bientityCondition.isEmpty() || cfg.bientityCondition.get().test(ctx));
    }

    public static boolean testItem(Entity holder, ResourceLocation receiver, ItemCtx ctx) {
        Config cfg = resolve(holder, receiver);
        return cfg != null && (cfg.itemCondition.isEmpty() || cfg.itemCondition.get().test(ctx));
    }
}
