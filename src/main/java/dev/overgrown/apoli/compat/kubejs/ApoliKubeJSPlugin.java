package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.overgrown.apoli.script.ApoliScripts;

public class ApoliKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(ApoliJSEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("Apoli", new ApoliScriptApi());
    }

    @Override
    public void init() {
        ApoliScripts.setBackend(new RhinoScriptBackend());
        ApoliScriptEvents.install();
    }
}
