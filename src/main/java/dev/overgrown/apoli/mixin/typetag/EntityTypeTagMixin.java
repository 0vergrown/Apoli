package dev.overgrown.apoli.mixin.typetag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyTypeTagPower;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityType.class)
public abstract class EntityTypeTagMixin {

    @ModifyReturnValue(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"))
    private boolean apoli$modifyTypeTag(boolean original, TagKey<EntityType<?>> tag) {
        if (!ModifyTypeTagPower.active()) return original;
        return ModifyTypeTagPower.resolve(original, (EntityType<?>) (Object) this, tag);
    }
}
