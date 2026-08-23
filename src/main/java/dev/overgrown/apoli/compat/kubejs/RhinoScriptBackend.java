package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Script;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.script.ApoliScriptConfig;
import dev.overgrown.apoli.script.ScriptBackend;
import dev.overgrown.apoli.script.ScriptCtx;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class RhinoScriptBackend implements ScriptBackend {

    private final Map<ResourceLocation, Script> compiled = new HashMap<>();
    private final Map<ResourceLocation, String> sources = new HashMap<>();

    @Nullable
    private ScriptableObject rootScope;

    @Override
    public String name() {
        return "Rhino (KubeJS)";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public void beginReload() {
        compiled.clear();
        sources.clear();
        rootScope = null;
    }

    @Override
    public void load(ResourceLocation id, String source) {
        sources.put(id, source);
    }

    @Override
    public void endReload() {
        if (sources.isEmpty()) return;
        Context cx = Context.enter();
        cx.setClassShutter(new SandboxShutter());
        try {
            ScriptableObject scope = cx.initSafeStandardObjects(null, true);
            ScriptableObject.putProperty(scope, "apoli", Context.javaToJS(cx, new ApoliScriptApi(), scope), cx);
            scope.sealObject(cx);
            for (Map.Entry<ResourceLocation, String> entry : sources.entrySet()) {
                try {
                    compiled.put(entry.getKey(),
                        cx.compileString(entry.getValue(), entry.getKey().toString(), 1, null));
                } catch (Throwable t) {
                    Apoli.LOGGER.error("[Apoli] Script {} failed to compile", entry.getKey(), t);
                }
            }
            rootScope = scope;
        } finally {
            sources.clear();
        }
    }

    @Override
    public boolean has(ResourceLocation id) {
        return compiled.containsKey(id);
    }

    @Override
    @Nullable
    public Object execute(ResourceLocation id, ScriptCtx ctx) {
        Script script = compiled.get(id);
        ScriptableObject root = rootScope;
        if (script == null || root == null) return null;
        Context cx = Context.enter();
        cx.setClassShutter(new SandboxShutter());
        try {
            cx.setInstructionObserverThreshold(ApoliScriptConfig.get().instructionBudget());
            ScriptableObject scope = (ScriptableObject) cx.newObject(root);
            scope.setPrototype(root);
            scope.setParentScope(null);
            ScriptableObject.putProperty(scope, "ctx", Context.javaToJS(cx, ctx, scope), cx);
            ScriptableObject.putProperty(scope, "params", Context.javaToJS(cx, ctx.getParams(), scope), cx);
            return script.exec(cx, scope);
        } catch (Throwable t) {
            throw t;
        }
    }
}
