package dev.overgrown.apoli.mixin.custom_model;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelPart.class)
@OnlyIn(Dist.CLIENT)
public interface ModelPartChildrenAccessor {
    @Accessor("children")
    Map<String, ModelPart> apoli$children();
}
