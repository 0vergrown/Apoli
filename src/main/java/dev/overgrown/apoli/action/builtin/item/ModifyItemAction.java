package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class ModifyItemAction implements ActionType<ItemCtx, ModifyItemAction.Cfg> {
    public record Cfg(ResourceLocation modifier) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("modifier").forGetter(Cfg::modifier)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, ItemCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return;
        ResourceKey<LootItemFunction> key = ResourceKey.create(Registries.ITEM_MODIFIER, cfg.modifier);
        LootItemFunction function = serverLevel.getServer().reloadableRegistries().lookup()
            .lookup(Registries.ITEM_MODIFIER)
            .flatMap(r -> r.get(key))
            .map(net.minecraft.core.Holder::value)
            .orElse(null);
        if (function == null) return;

        LootParams.Builder params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, ctx.holder() != null ? ctx.holder().position() : Vec3.ZERO)
            .withParameter(LootContextParams.TOOL, ctx.stack());
        if (ctx.holder() != null) {
            params.withParameter(LootContextParams.THIS_ENTITY, ctx.holder());
        }
        LootContext lootCtx = new LootContext.Builder(params.create(LootContextParamSets.ALL_PARAMS))
            .create(java.util.Optional.empty());
        ItemStack out = function.apply(ctx.stack(), lootCtx);
        ctx.stack().applyComponents(out.getComponents());
        ctx.stack().setCount(out.getCount());
        if (out.isDamageableItem()) ctx.stack().setDamageValue(out.getDamageValue());
    }
}
