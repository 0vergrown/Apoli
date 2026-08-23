package dev.overgrown.apoli.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.overgrown.apoli.script.ScriptCtx;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ApoliScriptEventJS extends EventJS {
    private final ScriptCtx ctx;

    public ApoliScriptEventJS(ScriptCtx ctx) {
        this.ctx = ctx;
    }

    public ScriptCtx getCtx() {
        return ctx;
    }

    @Nullable
    public Entity getEntity() {
        return ctx.getEntity();
    }

    @Nullable
    public Entity getTarget() {
        return ctx.getTarget();
    }

    @Nullable
    public Level getLevel() {
        return ctx.getLevel();
    }

    @Nullable
    public BlockPos getPos() {
        return ctx.getPos();
    }

    @Nullable
    public ItemStack getStack() {
        return ctx.getStack();
    }

    public CompoundTag getParams() {
        return ctx.getParams();
    }
}
