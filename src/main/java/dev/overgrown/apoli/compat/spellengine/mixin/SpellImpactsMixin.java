package dev.overgrown.apoli.compat.spellengine.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.overgrown.apoli.compat.spellengine.SpellEngineCompat;
import dev.overgrown.apoli.compat.spellengine.SpellImpactTracker;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.impact.SpellImpacts;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(SpellImpacts.class)
public abstract class SpellImpactsMixin {

    @WrapMethod(method = "performImpacts(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Ljava/util/List;Lnet/spell_engine/internals/SpellExecution$ImpactContext;ZLnet/spell_engine/api/spell/Spell$Impact$Action$Type;)Z")
    private static boolean apoli$trackImpacts(Level world, LivingEntity caster, @Nullable Entity target, Entity aoeSource,
                                              Holder<Spell> spell, List<Spell.Impact> impacts,
                                              SpellExecution.ImpactContext context, boolean additionalTargetLookup,
                                              @Nullable Spell.Impact.Action.Type filteredAction,
                                              Operation<Boolean> original) {
        SpellImpactTracker.push(spell);
        boolean performed;
        try {
            performed = original.call(world, caster, target, aoeSource, spell, impacts, context, additionalTargetLookup, filteredAction);
        } finally {
            SpellImpactTracker.pop();
        }
        if (performed && target != null && caster != null && !world.isClientSide()) {
            SpellEngineCompat.onImpact(caster, target, spell);
        }
        return performed;
    }
}
