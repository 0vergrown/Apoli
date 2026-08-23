package dev.overgrown.apoli.script;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ScriptCtx {
    private final Entity entity;
    private final Entity target;
    private final Level level;
    private final BlockPos pos;
    private final ItemStack stack;
    private final CompoundTag params;
    private final ResourceLocation power;

    private ScriptCtx(@Nullable Entity entity, @Nullable Entity target, @Nullable Level level,
                      @Nullable BlockPos pos, @Nullable ItemStack stack,
                      CompoundTag params, @Nullable ResourceLocation power) {
        this.entity = entity;
        this.target = target;
        this.level = level;
        this.pos = pos;
        this.stack = stack;
        this.params = params;
        this.power = power;
    }

    public static ScriptCtx of(EntityCtx ctx, CompoundTag params) {
        return new ScriptCtx(ctx.raw(), null, ctx.level(), null, null, params, null);
    }

    public static ScriptCtx of(BiEntityCtx ctx, CompoundTag params) {
        return new ScriptCtx(ctx.rawActor(), ctx.rawTarget(), ctx.level(), null, null, params, null);
    }

    public static ScriptCtx of(BlockCtx ctx, CompoundTag params) {
        return new ScriptCtx(null, null, ctx.level(), ctx.pos(), null, params, null);
    }

    public static ScriptCtx of(ItemCtx ctx, CompoundTag params) {
        return new ScriptCtx(ctx.holder(), null, ctx.level(), null, ctx.stack(), params, null);
    }

    public static ScriptCtx ofPower(PowerContainer holder, ResourceLocation powerId, CompoundTag params) {
        Entity owner = holder.rawOwner();
        return new ScriptCtx(owner, null, owner == null ? null : owner.level(), null, null, params, powerId);
    }

    @Nullable
    public Entity getEntity() {
        return entity;
    }

    @Nullable
    public LivingEntity getLiving() {
        return entity instanceof LivingEntity living ? living : null;
    }

    @Nullable
    public Entity getTarget() {
        return target;
    }

    @Nullable
    public Level getLevel() {
        return level;
    }

    @Nullable
    public ServerLevel getServerLevel() {
        return level instanceof ServerLevel server ? server : null;
    }

    @Nullable
    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public BlockState getBlockState() {
        return level == null || pos == null ? null : level.getBlockState(pos);
    }

    @Nullable
    public ItemStack getStack() {
        return stack;
    }

    public CompoundTag getParams() {
        return params;
    }

    @Nullable
    public ResourceLocation getPower() {
        return power;
    }

    @Nullable
    public PowerContainer getPowers() {
        return entity == null ? null : PowerContainer.of(entity);
    }

    public boolean isServer() {
        return level != null && !level.isClientSide();
    }
}
