package dev.overgrown.apoli.compat.spellengine.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.compat.spellengine.power.BypassSpellRequirementsPower;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.cost.Ammo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Ammo.class)
public abstract class AmmoMixin {

    @ModifyReturnValue(method = "ammoForSpell(Lnet/minecraft/world/entity/player/Player;Lnet/spell_engine/api/spell/Spell;Lnet/minecraft/world/item/ItemStack;)Lnet/spell_engine/internals/cost/Ammo$Result;",
        at = @At("RETURN"))
    private static Ammo.Result apoli$bypassItemCost(Ammo.Result result, Player player, Spell spell, ItemStack itemStack) {
        if (result == null || result.item() == null || (result.satisfied() && result.consume() == 0)) return result;
        if (!PowerLookup.holdsType(player, BypassSpellRequirementsPower.CANONICAL)) return result;
        Holder<Spell> holder = SpellRegistry.from(player.level()).wrapAsHolder(spell);
        if (!BypassSpellRequirementsPower.bypassesItems(player, holder)) return result;
        return new Ammo.Result(true, result.item(), 0, List.of());
    }
}
