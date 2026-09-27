package dev.overgrown.apoli.mixin.enchantment;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(Block.class)
public abstract class BlockDropsEnchantmentScopeMixin {

    @WrapMethod(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
        + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;"
        + "Lnet/minecraft/world/item/ItemStack;)V")
    private static void apoli$scopeDropResources(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity blockEntity,
                                                 @Nullable Entity entity, ItemStack tool, Operation<Void> original) {
        boolean scoped = ModifyEnchantmentLevelHandler.beginToolScope(entity, tool);
        try {
            original.call(state, level, pos, blockEntity, entity, tool);
        } finally {
            if (scoped) ModifyEnchantmentLevelHandler.endToolScope();
        }
    }

    @WrapMethod(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;"
        + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;"
        + "Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;")
    private static List<ItemStack> apoli$scopeGetDrops(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity,
                                                       @Nullable Entity entity, ItemStack tool, Operation<List<ItemStack>> original) {
        boolean scoped = ModifyEnchantmentLevelHandler.beginToolScope(entity, tool);
        try {
            return original.call(state, level, pos, blockEntity, entity, tool);
        } finally {
            if (scoped) ModifyEnchantmentLevelHandler.endToolScope();
        }
    }
}
