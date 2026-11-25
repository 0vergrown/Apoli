package dev.overgrown.apoli.compat.spellengine.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.compat.spellengine.SpellEngineCompat;
import dev.overgrown.apoli.compat.spellengine.power.BypassSpellRequirementsPower;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.casting.SpellCasting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpellCasting.class)
public abstract class SpellCastingMixin {

    @ModifyExpressionValue(method = "attempt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/spell_engine/internals/casting/SpellCast$Attempt;",
        at = @At(value = "INVOKE", target = "Lnet/spell_engine/internals/cost/SpellCooldownManager;isCoolingDown(Lnet/minecraft/core/Holder;)Z"))
    private static boolean apoli$bypassCooldown(boolean coolingDown, Player player, ItemStack itemStack, ResourceLocation spellId,
                                                boolean checkAmmo) {
        if (!coolingDown || !PowerLookup.holdsType(player, BypassSpellRequirementsPower.CANONICAL)) return coolingDown;
        Holder<Spell> spell = SpellEngineCompat.spell(player.level(), spellId);
        return spell == null || !BypassSpellRequirementsPower.bypassesCooldown(player, spell);
    }
}
