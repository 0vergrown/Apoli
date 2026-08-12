package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class ModifyItemAction implements ActionType<ItemCtx, ModifyItemAction.Cfg> {
    public record Cfg(ResourceLocation modifier) {}

    private static final LootContextParamSet PARAMS = LootContextParamSet.builder()
        .optional(LootContextParams.THIS_ENTITY)
        .optional(LootContextParams.LAST_DAMAGE_PLAYER)
        .optional(LootContextParams.DAMAGE_SOURCE)
        .optional(LootContextParams.KILLER_ENTITY)
        .optional(LootContextParams.DIRECT_KILLER_ENTITY)
        .optional(LootContextParams.ORIGIN)
        .optional(LootContextParams.BLOCK_STATE)
        .optional(LootContextParams.BLOCK_ENTITY)
        .optional(LootContextParams.TOOL)
        .optional(LootContextParams.EXPLOSION_RADIUS)
        .build();

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("modifier").forGetter(Cfg::modifier)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, ItemCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return;
        MinecraftServer server = serverLevel.getServer();
        LootItemFunction function = server.getLootData().getElement(LootDataType.MODIFIER, cfg.modifier);
        if (function == null) return;

        LootParams.Builder params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, ctx.holder() != null ? ctx.holder().position() : Vec3.ZERO)
            .withParameter(LootContextParams.TOOL, ctx.stack());
        if (ctx.holder() != null) {
            params.withParameter(LootContextParams.THIS_ENTITY, ctx.holder());
        }
        LootContext lootCtx = new LootContext.Builder(params.create(PARAMS))
            .create(null);
        ItemStack out = function.apply(ctx.stack(), lootCtx);
        if (out == ctx.stack()) return;
        if (ctx.replace(out)) return;
        ctx.stack().setTag(out.getTag());
        ctx.stack().setCount(out.getCount());
        if (out.isDamageableItem()) ctx.stack().setDamageValue(out.getDamageValue());
    }
}
