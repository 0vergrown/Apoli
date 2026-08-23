package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;

public interface ApoliJSEvents {
    EventGroup GROUP = EventGroup.of("ApoliEvents");

    EventHandler ENTITY_ACTION = GROUP.server("entityAction", () -> ApoliScriptEventJS.class).extra(Extra.REQUIRES_ID);
    EventHandler BIENTITY_ACTION = GROUP.server("bientityAction", () -> ApoliScriptEventJS.class).extra(Extra.REQUIRES_ID);
    EventHandler BLOCK_ACTION = GROUP.server("blockAction", () -> ApoliScriptEventJS.class).extra(Extra.REQUIRES_ID);
    EventHandler ITEM_ACTION = GROUP.server("itemAction", () -> ApoliScriptEventJS.class).extra(Extra.REQUIRES_ID);
    EventHandler POWER_ADDED = GROUP.server("powerAdded", () -> ApoliScriptEventJS.class).extra(Extra.REQUIRES_ID);
    EventHandler POWER_REMOVED = GROUP.server("powerRemoved", () -> ApoliScriptEventJS.class).extra(Extra.REQUIRES_ID);

    EventHandler ENTITY_CONDITION = GROUP.server("entityCondition", () -> ApoliScriptEventJS.class).hasResult().extra(Extra.REQUIRES_ID);
    EventHandler BIENTITY_CONDITION = GROUP.server("bientityCondition", () -> ApoliScriptEventJS.class).hasResult().extra(Extra.REQUIRES_ID);
    EventHandler BLOCK_CONDITION = GROUP.server("blockCondition", () -> ApoliScriptEventJS.class).hasResult().extra(Extra.REQUIRES_ID);
    EventHandler ITEM_CONDITION = GROUP.server("itemCondition", () -> ApoliScriptEventJS.class).hasResult().extra(Extra.REQUIRES_ID);
}
