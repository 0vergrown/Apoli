package dev.overgrown.apoli.compat.spellengine.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.compat.spellengine.power.ModifySpellPower;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// apoli:modify_spell. Each target is the overload the other one calls.
@Mixin(SpellParameters.class)
public abstract class SpellParametersMixin {
    @ModifyReturnValue(method = "getCastDuration(Lnet/minecraft/world/entity/LivingEntity;Lnet/spell_engine/api/spell/Spell;Lnet/minecraft/world/item/ItemStack;)F",
        at = @At("RETURN"))
    private static float apoli$castTime(float original, LivingEntity caster, Spell spell, ItemStack stack) {
        return ModifySpellPower.castTime(caster, spell, original);
    }

    @ModifyReturnValue(method = "getCooldownDuration(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)F",
        at = @At("RETURN"))
    private static float apoli$cooldown(float original, LivingEntity caster, Holder<Spell> spell, ItemStack stack) {
        return ModifySpellPower.cooldown(caster, spell, original);
    }

    @ModifyReturnValue(method = "getRangeCurved(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/Holder;F)F",
        at = @At("RETURN"))
    private static float apoli$range(float original, LivingEntity caster, Holder<Spell> spell, float base) {
        return ModifySpellPower.range(caster, spell, original);
    }
}
