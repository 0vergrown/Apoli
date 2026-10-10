package dev.overgrown.apoli.mixin.loot;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.access.KeyedLootTable;
import dev.overgrown.apoli.power.builtin.LootTableReplacement;
import dev.overgrown.apoli.power.builtin.ReplaceLootTablePower;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(LootTable.class)
public abstract class LootTableReplaceMixin implements KeyedLootTable {

    private static final String GET_RANDOM_ITEMS_RAW = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V";

    @Unique
    @Nullable
    private ResourceLocation apoli$key;

    @Override
    @Nullable
    public ResourceLocation apoli$key() {
        return this.apoli$key;
    }

    @Override
    public void apoli$setKey(ResourceLocation key) {
        this.apoli$key = key;
    }

    @Inject(method = GET_RANDOM_ITEMS_RAW, at = @At("HEAD"), cancellable = true)
    private void apoli$replaceTable(LootContext context, Consumer<ItemStack> consumer, CallbackInfo ci) {
        if (this.apoli$key == null || !ReplaceLootTablePower.HELD.isHeld()) return;
        LootTable self = (LootTable) (Object) this;
        if (LootTableReplacement.isStacked(self)) return;
        List<ResourceLocation> ids = new ArrayList<>(2);
        ReplaceLootTablePower.replacementsFor(context, this.apoli$key.toString(), ids);
        if (ids.isEmpty()) return;
        ReloadableServerRegistries.Holder registries = context.getLevel().getServer().reloadableRegistries();
        List<LootTable> tables = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            LootTable table = registries.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ids.get(i)));
            if (table == LootTable.EMPTY) {
                ReplaceLootTablePower.warnMissing(ids.get(i));
                continue;
            }
            tables.add(table);
        }
        if (tables.isEmpty()) return;
        LootTableReplacement.begin(self, tables);
        try {
            LootTableReplacement.peek().getRandomItemsRaw(context, consumer);
        } finally {
            LootTableReplacement.end();
        }
        ci.cancel();
    }

    @ModifyExpressionValue(method = GET_RANDOM_ITEMS_RAW, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/storage/loot/LootContext;pushVisitedElement(Lnet/minecraft/world/level/storage/loot/LootContext$VisitedEntry;)Z"))
    private boolean apoli$enterReplacement(boolean pushed) {
        if (pushed) LootTableReplacement.enter();
        return pushed;
    }

    @Inject(method = GET_RANDOM_ITEMS_RAW, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/storage/loot/LootContext;popVisitedElement(Lnet/minecraft/world/level/storage/loot/LootContext$VisitedEntry;)V"))
    private void apoli$exitReplacement(LootContext context, Consumer<ItemStack> consumer, CallbackInfo ci) {
        LootTableReplacement.exit();
    }
}
