package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.access.ModifierSummaryHolder;
import dev.overgrown.apoli.data.ModifierSummary;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

@Mixin(AttributeInstance.class)
public abstract class AttributeInstanceModifierSummaryMixin implements ModifierSummaryHolder {

    @Shadow
    @Final
    private Map<AttributeModifier.Operation, Set<AttributeModifier>> modifiersByOperation;

    @Unique
    private volatile ModifierSummary apoli$summary;

    @Inject(method = "setDirty", at = @At("HEAD"))
    private void apoli$forgetModifierSummary(CallbackInfo ci) {
        this.apoli$summary = null;
    }

    @Override
    public ModifierSummary apoli$modifierSummary() {
        ModifierSummary summary = this.apoli$summary;
        if (summary == null) {
            summary = new ModifierSummary(
                apoli$sum(AttributeModifier.Operation.ADDITION),
                apoli$sum(AttributeModifier.Operation.MULTIPLY_BASE),
                apoli$amounts(AttributeModifier.Operation.MULTIPLY_TOTAL));
            this.apoli$summary = summary;
        }
        return summary;
    }

    @Unique
    private double apoli$sum(AttributeModifier.Operation operation) {
        Set<AttributeModifier> modifiers = this.modifiersByOperation.get(operation);
        if (modifiers == null) return 0.0;
        double sum = 0.0;
        for (AttributeModifier modifier : modifiers) sum += modifier.getAmount();
        return sum;
    }

    @Unique
    private double[] apoli$amounts(AttributeModifier.Operation operation) {
        Set<AttributeModifier> modifiers = this.modifiersByOperation.get(operation);
        if (modifiers == null || modifiers.isEmpty()) return new double[0];
        double[] amounts = new double[modifiers.size()];
        int i = 0;
        for (AttributeModifier modifier : modifiers) amounts[i++] = modifier.getAmount();
        return amounts;
    }
}
