package dev.overgrown.apoli.mixin.modelparts;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ModelPart.class)
@Environment(EnvType.CLIENT)
public interface ModelPartCubesAccessor {

    @Accessor("cubes")
    List<ModelPart.Cube> apoli$modelCubes();
}
