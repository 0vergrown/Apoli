package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.mixin.recipe.CraftingMenuAccessAccessor;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ModifyCraftingHandler {

    private ModifyCraftingHandler() {}

    public static ItemStack modifyResult(Player player, Level level,
                                         @Nullable ResourceLocation recipeId, ItemStack original) {
        if (level.isClientSide() || original.isEmpty()) return original;
        ItemStack[] result = {original};
        PowerLookup.forEach(player, ApoliIds.MODIFY_CRAFTING, ModifyCraftingPower.Config.class, cfg -> {
            if (!applies(cfg, recipeId, result[0], level, player)) return;
            cfg.result().ifPresent(data -> result[0] = data.stack().copy());
            cfg.itemAction().ifPresent(a -> a.run(new ItemCtx(result[0], level, player)));
        });
        return result[0];
    }

    public static void afterCraft(Player player, ItemStack crafted, @Nullable ResourceLocation recipeId) {
        Level level = player.level();
        if (level.isClientSide()) return;
        BlockPos[] tablePos = {null};
        boolean[] resolvedPos = {false};
        PowerLookup.forEach(player, ApoliIds.MODIFY_CRAFTING, ModifyCraftingPower.Config.class, cfg -> {
            if (!applies(cfg, recipeId, crafted, level, player)) return;
            cfg.itemActionAfterCrafting().ifPresent(a -> a.run(new ItemCtx(crafted, level, player)));
            cfg.entityAction().ifPresent(a -> a.run(new EntityCtx(player, level)));
            if (cfg.blockAction().isPresent()) {
                if (!resolvedPos[0]) {
                    resolvedPos[0] = true;
                    tablePos[0] = craftingBlockPos(player);
                }
                if (tablePos[0] != null) {
                    cfg.blockAction().get().run(new BlockCtx(tablePos[0], level.getBlockState(tablePos[0]), level, player));
                }
            }
        });
    }

    private static boolean applies(ModifyCraftingPower.Config cfg, @Nullable ResourceLocation recipeId,
                                   ItemStack stack, Level level, Player player) {
        if (cfg.recipe().isPresent() && !cfg.recipe().get().equals(recipeId)) return false;
        return cfg.itemCondition().isEmpty() || cfg.itemCondition().get().test(new ItemCtx(stack, level, player));
    }

    private static @Nullable BlockPos craftingBlockPos(Player player) {
        if (!(player.containerMenu instanceof CraftingMenu menu)) return null;
        return ((CraftingMenuAccessAccessor) menu).apoli$access()
            .evaluate((level, pos) -> pos)
            .orElse(null);
    }
}
