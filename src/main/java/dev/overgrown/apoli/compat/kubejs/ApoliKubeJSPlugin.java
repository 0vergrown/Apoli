package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.overgrown.apoli.script.ApoliScripts;

public class ApoliKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void registerEvents() {
        ApoliJSEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("Apoli", new ApoliScriptApi());
    }

    @Override
    public void init() {
        ApoliScripts.setBackend(new RhinoScriptBackend());
        ApoliScriptEvents.install();
    }
}
