package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
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
    private ApoliContextFactory factory;

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
        factory = new ApoliContextFactory();
    }

    @Override
    public void load(ResourceLocation id, String source) {
        sources.put(id, source);
    }

    @Override
    public void endReload() {
        if (sources.isEmpty()) return;
        BudgetContext cx = enter();
        try {
            ScriptableObject scope = sandboxScope(cx);
            for (Map.Entry<ResourceLocation, String> entry : sources.entrySet()) {
                try {
                    compiled.put(entry.getKey(),
                        cx.compileString(entry.getValue(), entry.getKey().toString(), 1, null));
                } catch (Throwable t) {
                    Apoli.LOGGER.error("[Apoli] Script {} failed to compile", entry.getKey(), t);
                }
            }
            this.rootScope = scope;
        } finally {
            cx.disarm();
        }
        sources.clear();
    }

    @Nullable
    private ScriptableObject rootScope;

    @Override
    public boolean has(ResourceLocation id) {
        return compiled.containsKey(id);
    }

    @Override
    @Nullable
    public Object execute(ResourceLocation id, ScriptCtx ctx) {
        Script script = compiled.get(id);
        if (script == null || rootScope == null) return null;
        BudgetContext cx = enter();
        try {
            cx.arm(ApoliScriptConfig.get().scriptTimeoutMillis());
            ScriptableObject scope = (ScriptableObject) cx.newObject(rootScope);
            scope.setPrototype(rootScope);
            scope.setParentScope(null);
            ScriptableObject.putProperty(scope, "ctx", cx.javaToJS(ctx, scope), cx);
            ScriptableObject.putProperty(scope, "params", cx.javaToJS(ctx.getParams(), scope), cx);
            return script.exec(cx, scope);
        } finally {
            cx.disarm();
        }
    }

    private BudgetContext enter() {
        if (factory == null) factory = new ApoliContextFactory();
        BudgetContext cx = (BudgetContext) factory.enter();
        cx.setInstructionObserverThreshold(10_000);
        return cx;
    }

    private ScriptableObject sandboxScope(BudgetContext cx) {
        ScriptableObject scope = cx.initSafeStandardObjects(null, true);
        ScriptableObject.putProperty(scope, "apoli", cx.javaToJS(new ApoliScriptApi(), scope), cx);
        scope.sealObject(cx);
        return scope;
    }

    private static final class ApoliContextFactory extends ContextFactory {
        private final SandboxClassStorage storage = new SandboxClassStorage();

        @Override
        protected Context createContext() {
            return new BudgetContext(this);
        }

        @Override
        public dev.latvian.mods.rhino.CachedClassStorage getCachedClassStorage() {
            return storage;
        }
    }

    private static final class BudgetContext extends Context {
        private long deadlineNanos;

        BudgetContext(ContextFactory factory) {
            super(factory);
        }

        void arm(long millis) {
            this.deadlineNanos = System.nanoTime() + millis * 1_000_000L;
        }

        void disarm() {
            this.deadlineNanos = 0L;
        }

        @Override
        protected void observeInstructionCount(int instructionCount) {
            if (deadlineNanos != 0L && System.nanoTime() > deadlineNanos) {
                throw new IllegalStateException("[Apoli] script exceeded its time budget and was stopped");
            }
        }
    }
}
