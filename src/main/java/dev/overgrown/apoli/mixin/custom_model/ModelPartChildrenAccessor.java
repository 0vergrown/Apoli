package dev.overgrown.apoli.mixin.custom_model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelPart.class)
@Environment(EnvType.CLIENT)
public interface ModelPartChildrenAccessor {
    @Accessor("children")
    Map<String, ModelPart> apoli$children();
}
