package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventTargetType;
import dev.latvian.mods.kubejs.event.TargetedEventHandler;
import net.minecraft.resources.ResourceLocation;

public interface ApoliJSEvents {
    EventGroup GROUP = EventGroup.of("ApoliEvents");

    TargetedEventHandler<ResourceLocation> ENTITY_ACTION =
        GROUP.server("entityAction", () -> ApoliScriptKubeEvent.class).requiredTarget(EventTargetType.ID);
    TargetedEventHandler<ResourceLocation> BIENTITY_ACTION =
        GROUP.server("bientityAction", () -> ApoliScriptKubeEvent.class).requiredTarget(EventTargetType.ID);
    TargetedEventHandler<ResourceLocation> BLOCK_ACTION =
        GROUP.server("blockAction", () -> ApoliScriptKubeEvent.class).requiredTarget(EventTargetType.ID);
    TargetedEventHandler<ResourceLocation> ITEM_ACTION =
        GROUP.server("itemAction", () -> ApoliScriptKubeEvent.class).requiredTarget(EventTargetType.ID);

    TargetedEventHandler<ResourceLocation> ENTITY_CONDITION =
        GROUP.server("entityCondition", () -> ApoliScriptKubeEvent.class).hasResult().requiredTarget(EventTargetType.ID);
    TargetedEventHandler<ResourceLocation> BIENTITY_CONDITION =
        GROUP.server("bientityCondition", () -> ApoliScriptKubeEvent.class).hasResult().requiredTarget(EventTargetType.ID);
    TargetedEventHandler<ResourceLocation> BLOCK_CONDITION =
        GROUP.server("blockCondition", () -> ApoliScriptKubeEvent.class).hasResult().requiredTarget(EventTargetType.ID);
    TargetedEventHandler<ResourceLocation> ITEM_CONDITION =
        GROUP.server("itemCondition", () -> ApoliScriptKubeEvent.class).hasResult().requiredTarget(EventTargetType.ID);

    TargetedEventHandler<ResourceLocation> POWER_ADDED =
        GROUP.server("powerAdded", () -> ApoliScriptKubeEvent.class).requiredTarget(EventTargetType.ID);
    TargetedEventHandler<ResourceLocation> POWER_REMOVED =
        GROUP.server("powerRemoved", () -> ApoliScriptKubeEvent.class).requiredTarget(EventTargetType.ID);
}
