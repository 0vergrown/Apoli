package dev.overgrown.apoli.mixin.loot;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.LootTableReplacement;
import dev.overgrown.apoli.power.builtin.ReplaceLootTablePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LootTableReference.class)
public abstract class LootTableReferenceReplacedMixin {

    @Shadow
    @Final
    ResourceLocation name;

    @ModifyExpressionValue(method = "createItemStack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/storage/loot/LootDataResolver;getLootTable(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/world/level/storage/loot/LootTable;"))
    private LootTable apoli$replacedTable(LootTable original) {
        return LootTableReplacement.isActive() && this.name.equals(ReplaceLootTablePower.REPLACED_TABLE)
            ? LootTableReplacement.peek()
            : original;
    }
}
