package dev.overgrown.apoli.mixin.loot;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.access.KeyedLootTable;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LootDataManager.class)
public abstract class LootDataManagerKeyMixin {

    @ModifyReturnValue(method = "getElement(Lnet/minecraft/world/level/storage/loot/LootDataId;)Ljava/lang/Object;", at = @At("RETURN"))
    private Object apoli$keyLootTable(Object element, @Local(argsOnly = true) LootDataId<?> id) {
        if (element instanceof LootTable table && table != LootTable.EMPTY) {
            KeyedLootTable keyed = (KeyedLootTable) table;
            if (keyed.apoli$key() == null) keyed.apoli$setKey(id.location());
        }
        return element;
    }
}
