package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.TargetedEventHandler;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import net.minecraft.resources.ResourceLocation;

public final class ApoliScriptEvents {
    private ApoliScriptEvents() {}

    public static void install() {
        bindAction(ApoliScripts.Kind.ENTITY_ACTION, ApoliJSEvents.ENTITY_ACTION);
        bindAction(ApoliScripts.Kind.BIENTITY_ACTION, ApoliJSEvents.BIENTITY_ACTION);
        bindAction(ApoliScripts.Kind.BLOCK_ACTION, ApoliJSEvents.BLOCK_ACTION);
        bindAction(ApoliScripts.Kind.ITEM_ACTION, ApoliJSEvents.ITEM_ACTION);
        bindAction(ApoliScripts.Kind.POWER_ADDED, ApoliJSEvents.POWER_ADDED);
        bindAction(ApoliScripts.Kind.POWER_REMOVED, ApoliJSEvents.POWER_REMOVED);

        bindCondition(ApoliScripts.Kind.ENTITY_CONDITION, ApoliJSEvents.ENTITY_CONDITION);
        bindCondition(ApoliScripts.Kind.BIENTITY_CONDITION, ApoliJSEvents.BIENTITY_CONDITION);
        bindCondition(ApoliScripts.Kind.BLOCK_CONDITION, ApoliJSEvents.BLOCK_CONDITION);
        bindCondition(ApoliScripts.Kind.ITEM_CONDITION, ApoliJSEvents.ITEM_CONDITION);
    }

    private static void bindAction(ApoliScripts.Kind kind, TargetedEventHandler<ResourceLocation> handler) {
        ApoliScripts.setEventDispatcher(kind, (id, ctx) -> {
            if (!handler.hasListeners(id)) return false;
            handler.post(new ApoliScriptKubeEvent(ctx), id);
            return true;
        });
    }

    private static void bindCondition(ApoliScripts.Kind kind, TargetedEventHandler<ResourceLocation> handler) {
        ApoliScripts.setEventPredicate(kind, (id, ctx) -> {
            if (!handler.hasListeners(id)) return null;
            EventResult result = handler.post(new ApoliScriptKubeEvent(ctx), id);
            return result.interruptTrue();
        });
    }
}
