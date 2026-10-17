package dev.overgrown.apoli.mixin.typetag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyTypeTagPower;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(EntityType.class)
public abstract class EntityTypeTagMixin {

    @ModifyReturnValue(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"))
    private boolean apoli$modifyTypeTag(boolean original, TagKey<EntityType<?>> tag) {
        if (!ModifyTypeTagPower.active()) return original;
        return ModifyTypeTagPower.resolve(original, (EntityType<?>) (Object) this, tag);
    }

    @ModifyReturnValue(method = "is(Lnet/minecraft/core/HolderSet;)Z", at = @At("RETURN"))
    private boolean apoli$modifyTypeTagSet(boolean original, HolderSet<EntityType<?>> holders) {
        if (!ModifyTypeTagPower.active()) return original;
        TagKey<EntityType<?>> tag;
        if (holders instanceof HolderSet.Named<EntityType<?>> named) {
            tag = named.key();
        } else {
            Optional<TagKey<EntityType<?>>> key = holders.unwrapKey();
            if (key.isEmpty()) return original;
            tag = key.get();
        }
        return ModifyTypeTagPower.resolve(original, (EntityType<?>) (Object) this, tag);
    }
}
