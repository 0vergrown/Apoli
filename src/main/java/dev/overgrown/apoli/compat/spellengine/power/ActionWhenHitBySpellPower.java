package dev.overgrown.apoli.compat.spellengine.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.Spell;

public final class ActionWhenHitBySpellPower extends PowerType<ActionOnSpellHitPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("action_when_hit_by_spell");

    @Override
    public MapCodec<ActionOnSpellHitPower.Config> configCodec() {
        return ActionOnSpellHitPower.CODEC;
    }

    public static void handle(LivingEntity caster, Entity target, Holder<Spell> spell) {
        if (!PowerLookup.holdsType(target, CANONICAL)) return;
        ActionOnSpellHitPower.run(target, CANONICAL, caster, target, spell);
    }
}
