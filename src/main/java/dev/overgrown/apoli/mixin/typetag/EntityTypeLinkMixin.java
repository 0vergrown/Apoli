package dev.overgrown.apoli.mixin.typetag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyTypeTagPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityTypeLinkMixin {

    @ModifyReturnValue(method = "getType()Lnet/minecraft/world/entity/EntityType;", at = @At("RETURN"))
    private EntityType<?> apoli$linkTypeToEntity(EntityType<?> original) {
        if (ModifyTypeTagPower.active()) {
            ModifyTypeTagPower.link((Entity) (Object) this);
        }
        return original;
    }
}
