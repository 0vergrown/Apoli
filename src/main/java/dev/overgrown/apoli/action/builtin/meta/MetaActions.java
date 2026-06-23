package dev.overgrown.apoli.action.builtin.meta;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.action.TypedActionRegistry;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import net.minecraft.resources.ResourceLocation;

public final class MetaActions {
    private MetaActions() {}

    private static final ResourceLocation AND = Apoli.id("and");
    private static final ResourceLocation RANDOM_CHANCE = Apoli.id("random_chance");
    private static final ResourceLocation CHOICE = Apoli.id("choice");
    private static final ResourceLocation DELAY = Apoli.id("delay");
    private static final ResourceLocation LOOP = Apoli.id("loop");
    private static final ResourceLocation IF_ELSE = Apoli.id("if_else");
    private static final ResourceLocation IF_ELSE_LIST = Apoli.id("if_else_list");

    private static AliasingOptions chanceAliases() {
        return AliasingOptions.builder().addTypeAlias(Apoli.id("chance")).build();
    }

    public static void registerEntity() {
        TypedActionRegistry<dev.overgrown.apoli.condition.context.EntityCtx> reg = ActionTypes.ENTITY;
        reg.register(AND, new AndMetaAction<>(EntityAction.CODEC, EntityAction::run));
        reg.register(RANDOM_CHANCE, new RandomChanceMetaAction<>(EntityAction.CODEC, EntityAction::run), chanceAliases());
        reg.register(CHOICE, new ChoiceMetaAction<>(EntityAction.CODEC, EntityAction::run));
        reg.register(DELAY, new DelayMetaAction<>(EntityAction.CODEC, EntityAction::run));
        reg.register(LOOP, new LoopMetaAction<>(EntityAction.CODEC, EntityAction::run));
        reg.register(IF_ELSE, new IfElseMetaAction<>(EntityCondition.CODEC, EntityAction.CODEC,
            EntityCondition::test, EntityAction::run));
        reg.register(IF_ELSE_LIST, new IfElseListMetaAction<>(EntityCondition.CODEC, EntityAction.CODEC,
            EntityCondition::test, EntityAction::run));
    }

    public static void registerBiEntity() {
        TypedActionRegistry<dev.overgrown.apoli.condition.context.BiEntityCtx> reg = ActionTypes.BI_ENTITY;
        reg.register(AND, new AndMetaAction<>(BiEntityAction.CODEC, BiEntityAction::run));
        reg.register(RANDOM_CHANCE, new RandomChanceMetaAction<>(BiEntityAction.CODEC, BiEntityAction::run), chanceAliases());
        reg.register(CHOICE, new ChoiceMetaAction<>(BiEntityAction.CODEC, BiEntityAction::run));
        reg.register(DELAY, new DelayMetaAction<>(BiEntityAction.CODEC, BiEntityAction::run));
        reg.register(LOOP, new LoopMetaAction<>(BiEntityAction.CODEC, BiEntityAction::run));
        reg.register(IF_ELSE, new IfElseMetaAction<>(BiEntityCondition.CODEC, BiEntityAction.CODEC,
            BiEntityCondition::test, BiEntityAction::run));
        reg.register(IF_ELSE_LIST, new IfElseListMetaAction<>(BiEntityCondition.CODEC, BiEntityAction.CODEC,
            BiEntityCondition::test, BiEntityAction::run));
    }

    public static void registerBlock() {
        TypedActionRegistry<dev.overgrown.apoli.condition.context.BlockCtx> reg = ActionTypes.BLOCK;
        reg.register(AND, new AndMetaAction<>(BlockAction.CODEC, BlockAction::run));
        reg.register(RANDOM_CHANCE, new RandomChanceMetaAction<>(BlockAction.CODEC, BlockAction::run), chanceAliases());
        reg.register(CHOICE, new ChoiceMetaAction<>(BlockAction.CODEC, BlockAction::run));
        reg.register(DELAY, new DelayMetaAction<>(BlockAction.CODEC, BlockAction::run));
        reg.register(LOOP, new LoopMetaAction<>(BlockAction.CODEC, BlockAction::run));
        reg.register(IF_ELSE, new IfElseMetaAction<>(BlockCondition.CODEC, BlockAction.CODEC,
            BlockCondition::test, BlockAction::run));
        reg.register(IF_ELSE_LIST, new IfElseListMetaAction<>(BlockCondition.CODEC, BlockAction.CODEC,
            BlockCondition::test, BlockAction::run));
    }

    public static void registerItem() {
        TypedActionRegistry<dev.overgrown.apoli.condition.context.ItemCtx> reg = ActionTypes.ITEM;
        reg.register(AND, new AndMetaAction<>(ItemAction.CODEC, ItemAction::run));
        reg.register(RANDOM_CHANCE, new RandomChanceMetaAction<>(ItemAction.CODEC, ItemAction::run), chanceAliases());
        reg.register(CHOICE, new ChoiceMetaAction<>(ItemAction.CODEC, ItemAction::run));
        reg.register(DELAY, new DelayMetaAction<>(ItemAction.CODEC, ItemAction::run));
        reg.register(LOOP, new LoopMetaAction<>(ItemAction.CODEC, ItemAction::run));
        reg.register(IF_ELSE, new IfElseMetaAction<>(ItemCondition.CODEC, ItemAction.CODEC,
            ItemCondition::test, ItemAction::run));
        reg.register(IF_ELSE_LIST, new IfElseListMetaAction<>(ItemCondition.CODEC, ItemAction.CODEC,
            ItemCondition::test, ItemAction::run));
    }
}
