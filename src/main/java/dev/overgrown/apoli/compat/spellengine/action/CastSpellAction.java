package dev.overgrown.apoli.compat.spellengine.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.compat.spellengine.SpellEngineCompat;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.Spell;

public final class CastSpellAction implements ActionType<EntityCtx, CastSpellAction.Cfg> {
    public record Cfg(ResourceLocation spell) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("spell").forGetter(Cfg::spell)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.raw() instanceof LivingEntity caster) || caster.level().isClientSide()) return;
        Holder<Spell> spell = SpellEngineCompat.spell(caster.level(), cfg.spell());
        if (spell == null) {
            SpellEngineCompat.warnUnknownSpell(cfg.spell());
            return;
        }
        SpellEngineCompat.cast(caster, spell);
    }
}
