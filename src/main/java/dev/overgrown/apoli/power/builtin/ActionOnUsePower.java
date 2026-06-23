package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.ActionResult;
import dev.overgrown.apoli.data.Hand;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class ActionOnUsePower extends PowerType<ActionOnUsePower.Config> {
    public record Config(
        Optional<BiEntityAction> bientityAction,
        Optional<ItemAction> heldItemAction,
        Optional<ItemAction> resultItemAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<ItemCondition> itemCondition,
        List<Hand> hands,
        Optional<ItemStackData> resultStack,
        ActionResult actionResult,
        boolean targetUsed
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Config::bientityAction),
            ItemAction.CODEC.optionalFieldOf("held_item_action").forGetter(Config::heldItemAction),
            ItemAction.CODEC.optionalFieldOf("result_item_action").forGetter(Config::resultItemAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            Hand.LIST_CODEC.optionalFieldOf("hands", Hand.BOTH).forGetter(Config::hands),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            ActionResult.CODEC.optionalFieldOf("action_result", ActionResult.SUCCESS).forGetter(Config::actionResult),
            Codec.BOOL.optionalFieldOf("target_used", false).forGetter(Config::targetUsed)
        ).apply(i, Config::new));
    }

    public InteractionResult tryFire(Config cfg, LivingEntity actor, LivingEntity target, InteractionHand hand) {
        if (!handAllowed(cfg, hand)) return InteractionResult.PASS;

        ItemStack stack = actor.getItemInHand(hand);
        ItemCtx itemCtx = new ItemCtx(stack, actor.level(), actor);
        if (cfg.itemCondition.isPresent() && !cfg.itemCondition.get().test(itemCtx)) {
            return InteractionResult.PASS;
        }
        BiEntityCtx biCtx = new BiEntityCtx(actor, target, actor.level());
        if (cfg.bientityCondition.isPresent() && !cfg.bientityCondition.get().test(biCtx)) {
            return InteractionResult.PASS;
        }
        cfg.bientityAction.ifPresent(a -> a.run(biCtx));
        cfg.heldItemAction.ifPresent(a -> a.run(itemCtx));
        if (cfg.resultStack.isPresent()) {
            giveResultStack(actor, hand, cfg.resultStack.get().stack().copy());
        }
        if (cfg.resultItemAction.isPresent() && actor.getItemInHand(hand) != ItemStack.EMPTY) {
            cfg.resultItemAction.get().run(new ItemCtx(actor.getItemInHand(hand), actor.level(), actor));
        }
        return cfg.actionResult.vanilla();
    }

    @Override
    public boolean isActive(net.minecraft.resources.ResourceLocation id, Config cfg, EntityCtx ctx) {
        return true;
    }

    private static boolean handAllowed(Config cfg, InteractionHand hand) {
        Hand needed = Hand.of(hand);
        for (Hand h : cfg.hands) if (h == needed) return true;
        return false;
    }

    private static void giveResultStack(LivingEntity actor, InteractionHand hand, ItemStack result) {
        ItemStack held = actor.getItemInHand(hand);
        if (held.isEmpty()) {
            actor.setItemInHand(hand, result);
        } else if (actor instanceof net.minecraft.world.entity.player.Player p) {
            if (!p.getInventory().add(result)) {
                p.drop(result, false);
            }
        }
    }
}
