package dev.overgrown.apoli.mixin.custom_model;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.render.ModelPartNames;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PartDefinition.class)
@OnlyIn(Dist.CLIENT)
public abstract class PartDefinitionNamesMixin {
    @ModifyReturnValue(method = "bake", at = @At("RETURN"))
    private ModelPart apoli$recordPartNames(ModelPart baked) {
        ModelPartNames.recordBaked(baked);
        return baked;
    }
}
