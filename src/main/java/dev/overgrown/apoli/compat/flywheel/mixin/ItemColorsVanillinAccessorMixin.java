package dev.overgrown.apoli.compat.flywheel.mixin;

import dev.engine_room.vanillin.neoforge.mixin.item.ItemColorsAccessor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(ItemColors.class)
public abstract class ItemColorsVanillinAccessorMixin implements ItemColorsAccessor {

    @Shadow
    @Final
    private Map<Item, ItemColor> itemColors;

    @Override
    public Map<Item, ItemColor> vanillin$itemColors() {
        return this.itemColors;
    }
}
