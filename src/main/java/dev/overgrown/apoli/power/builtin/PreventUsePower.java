package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Hand;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class PreventUsePower extends PowerType<PreventUsePower.Config> {
    public record Config(
        Optional<BiEntityAction> bientityAction,
        Optional<ItemAction> heldItemAction,
        Optional<ItemAction> resultItemAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<ItemCondition> itemCondition,
        List<Hand> hands,
        Optional<ItemStackData> resultStack,
        boolean targetUsed
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("held_item_action", ItemAction.CODEC).forGetter(Config::heldItemAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("result_item_action", ItemAction.CODEC).forGetter(Config::resultItemAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            Hand.LIST_CODEC.optionalFieldOf("hands", Hand.BOTH).forGetter(Config::hands),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            Codec.BOOL.optionalFieldOf("target_used", false).forGetter(Config::targetUsed)
        ).apply(i, Config::new));
    }

    public boolean tryPrevent(Config cfg, LivingEntity actor, Entity target, InteractionHand hand, boolean runActions) {
        if (!BlockInteractionHelper.handMatches(cfg.hands, hand)) return false;

        ItemStack stack = actor.getItemInHand(hand);
        ItemCtx itemCtx = new ItemCtx(stack, actor.level(), actor);
        if (cfg.itemCondition.isPresent() && !cfg.itemCondition.get().test(itemCtx)) return false;

        BiEntityCtx biCtx = new BiEntityCtx(actor, target, actor.level());
        if (cfg.bientityCondition.isPresent() && !cfg.bientityCondition.get().test(biCtx)) return false;

        if (runActions) {
            cfg.bientityAction.ifPresent(action -> action.run(biCtx));
            cfg.heldItemAction.ifPresent(action -> action.run(itemCtx));
            if (cfg.resultStack.isPresent()) {
                ActionOnUsePower.giveResultStack(actor, hand, cfg.resultStack.get().stack().copy());
            }
            if (cfg.resultItemAction.isPresent() && !actor.getItemInHand(hand).isEmpty()) {
                cfg.resultItemAction.get().run(new ItemCtx(actor.getItemInHand(hand), actor.level(), actor));
            }
        }
        return true;
    }

    public static dev.overgrown.apoli.alias.AliasDefault<Boolean> targetUsed(boolean value) {
        return dev.overgrown.apoli.alias.AliasDefault.of("target_used", com.mojang.serialization.Codec.BOOL, value);
    }
}
