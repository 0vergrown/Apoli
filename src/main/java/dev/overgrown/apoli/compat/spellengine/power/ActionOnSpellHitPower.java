package dev.overgrown.apoli.compat.spellengine.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.Spell;

import java.util.Optional;

public final class ActionOnSpellHitPower extends PowerType<ActionOnSpellHitPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("action_on_spell_hit");

    public record Config(SpellFilter filter, Optional<BiEntityAction> bientityAction,
                         Optional<BiEntityCondition> bientityCondition) {}

    public static final MapCodec<Config> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SpellFilter.MAP_CODEC.forGetter(Config::filter),
        LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityAction),
        LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
    ).apply(i, Config::new));

    @Override
    public MapCodec<Config> configCodec() {
        return CODEC;
    }

    public static void handle(LivingEntity caster, Entity target, Holder<Spell> spell) {
        if (!PowerLookup.holdsType(caster, CANONICAL)) return;
        run(caster, CANONICAL, caster, target, spell);
    }

    static void run(Entity holder, ResourceLocation type, Entity caster, Entity target, Holder<Spell> spell) {
        PowerLookup.forEach(holder, type, Config.class, cfg -> {
            if (!cfg.filter().test(spell)) return;
            BiEntityCtx ctx = BiEntityCtx.of(caster, target, holder.level());
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(ctx)) return;
            cfg.bientityAction().ifPresent(action -> action.run(ctx));
        });
    }
}
