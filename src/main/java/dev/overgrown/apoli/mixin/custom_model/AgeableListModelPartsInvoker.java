package dev.overgrown.apoli.mixin.custom_model;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AgeableListModel.class)
@OnlyIn(Dist.CLIENT)
public interface AgeableListModelPartsInvoker {
    @Invoker("headParts")
    Iterable<ModelPart> apoli$headParts();

    @Invoker("bodyParts")
    Iterable<ModelPart> apoli$bodyParts();
}
