package dev.overgrown.apoli.condition.context;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record ItemCtx(ItemStack stack, Level level, @Nullable LivingEntity holder,
                      @Nullable Consumer<ItemStack> replacer) {

    public ItemCtx(ItemStack stack, Level level, @Nullable LivingEntity holder) {
        this(stack, level, holder, null);
    }

    public boolean replace(ItemStack replacement) {
        if (replacer == null) return false;
        replacer.accept(replacement);
        return true;
    }
}
