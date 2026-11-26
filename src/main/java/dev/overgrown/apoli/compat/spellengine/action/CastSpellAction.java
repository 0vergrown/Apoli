package dev.overgrown.apoli.compat.spellengine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.compat.spellengine.SpellEngineCompat;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.Spell;
import org.jetbrains.annotations.Nullable;

// Casts a spell at once. As an entity action it aims where the caster looks; as a bi-entity action it hits the target.
// use_cost: the cast uses the spell's cost and starts its cooldown, like a normal cast (players only).
public final class CastSpellAction implements ActionType<EntityCtx, CastSpellAction.Cfg> {
    public record Cfg(ResourceLocation spell, boolean useCost) {}

    static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        IdCodecs.ID.fieldOf("spell").forGetter(Cfg::spell),
        Codec.BOOL.optionalFieldOf("use_cost", false).forGetter(Cfg::useCost)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        cast(cfg, ctx.raw(), null);
    }

    static void cast(Cfg cfg, @Nullable Entity entity, @Nullable Entity target) {
        if (!(entity instanceof LivingEntity caster) || caster.level().isClientSide()) return;
        Holder<Spell> spell = SpellEngineCompat.spell(caster.level(), cfg.spell());
        if (spell == null) {
            SpellEngineCompat.warnUnknownSpell(cfg.spell());
            return;
        }
        SpellEngineCompat.cast(caster, spell, target, cfg.useCost());
    }

    public static final class BiEntity implements ActionType<BiEntityCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return CODEC;
        }

        @Override
        public void run(Cfg cfg, BiEntityCtx ctx) {
            if (ctx.target() != null) cast(cfg, ctx.actor(), ctx.target());
        }
    }
}
