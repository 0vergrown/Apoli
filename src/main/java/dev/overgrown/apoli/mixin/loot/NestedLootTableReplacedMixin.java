package dev.overgrown.apoli.mixin.loot;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.datafixers.util.Either;
import dev.overgrown.apoli.power.builtin.LootTableReplacement;
import dev.overgrown.apoli.power.builtin.ReplaceLootTablePower;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(NestedLootTable.class)
public abstract class NestedLootTableReplacedMixin {

    @Shadow
    @Final
    private Either<ResourceKey<LootTable>, LootTable> contents;

    @ModifyExpressionValue(method = "createItemStack", at = @At(value = "INVOKE",
        target = "Lcom/mojang/datafixers/util/Either;map(Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/lang/Object;"))
    private Object apoli$replacedTable(Object original) {
        if (!LootTableReplacement.isActive()) return original;
        Optional<ResourceKey<LootTable>> key = this.contents.left();
        return key.isPresent() && key.get().location().equals(ReplaceLootTablePower.REPLACED_TABLE)
            ? LootTableReplacement.peek()
            : original;
    }
}
