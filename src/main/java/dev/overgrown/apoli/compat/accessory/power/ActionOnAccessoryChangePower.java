package dev.overgrown.apoli.compat.accessory.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.compat.accessory.AccessorySlotRef;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;


public final class ActionOnAccessoryChangePower extends PowerType<ActionOnAccessoryChangePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("action_on_accessory_change");

    public record Config(
        Optional<EntityAction> entityActionOnEquip,
        Optional<ItemAction> itemActionOnEquip,
        Optional<EntityAction> entityActionOnUnequip,
        Optional<ItemAction> itemActionOnUnequip,
        Optional<ItemCondition> itemCondition,
        List<AccessorySlot> slots
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.optionalFieldOf("entity_action_on_equip").forGetter(Config::entityActionOnEquip),
            ItemAction.CODEC.optionalFieldOf("item_action_on_equip").forGetter(Config::itemActionOnEquip),
            EntityAction.CODEC.optionalFieldOf("entity_action_on_unequip").forGetter(Config::entityActionOnUnequip),
            ItemAction.CODEC.optionalFieldOf("item_action_on_unequip").forGetter(Config::itemActionOnUnequip),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            AccessorySlot.LIST.optionalFieldOf("slots", List.of()).forGetter(Config::slots)
        ).apply(i, Config::new));
    }

    
    public static void handle(LivingEntity entity, AccessorySlotRef ref, ItemStack stack, boolean equipping) {
        if (entity.level().isClientSide()) return;
        ItemCtx itemCtx = new ItemCtx(stack, entity.level(), entity);
        EntityCtx entityCtx = EntityCtx.of(entity, entity.level());
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (!AccessorySlot.matchesAny(cfg.slots(), ref)) return;
            if (cfg.itemCondition().isPresent() && !cfg.itemCondition().get().test(itemCtx)) return;
            if (equipping) {
                cfg.entityActionOnEquip().ifPresent(a -> a.run(entityCtx));
                cfg.itemActionOnEquip().ifPresent(a -> a.run(itemCtx));
            } else {
                cfg.entityActionOnUnequip().ifPresent(a -> a.run(entityCtx));
                cfg.itemActionOnUnequip().ifPresent(a -> a.run(itemCtx));
            }
        });
    }
}
