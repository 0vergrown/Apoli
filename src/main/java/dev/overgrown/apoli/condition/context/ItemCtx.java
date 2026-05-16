package dev.overgrown.apoli.condition.context;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record ItemCtx(ItemStack stack, Level level, @Nullable LivingEntity holder) {}
