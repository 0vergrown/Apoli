package dev.overgrown.apoli.mixin.flag;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntitySharedFlagInvoker {

    @Invoker("getSharedFlag")
    boolean apoli$getSharedFlag(int flag);
}
