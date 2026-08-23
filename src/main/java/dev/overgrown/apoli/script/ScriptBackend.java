package dev.overgrown.apoli.script;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface ScriptBackend {
    String name();

    boolean available();

    void beginReload();

    void load(ResourceLocation id, String source);

    void endReload();

    boolean has(ResourceLocation id);

    @Nullable
    Object execute(ResourceLocation id, ScriptCtx ctx);
}
