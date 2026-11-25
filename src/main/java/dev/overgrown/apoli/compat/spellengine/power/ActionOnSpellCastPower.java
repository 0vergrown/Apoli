package dev.overgrown.apoli.compat.spellengine.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.spell_engine.api.spell.Spell;

import java.util.List;
import java.util.Optional;

public final class ActionOnSpellCastPower extends PowerType<ActionOnSpellCastPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("action_on_spell_cast");

    public record Config(SpellFilter filter, Optional<EntityAction> entityAction, Optional<BiEntityAction> bientityAction,
                         Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            SpellFilter.MAP_CODEC.forGetter(Config::filter),
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityAction),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }

    public static void handle(LivingEntity caster, Holder<Spell> spell, List<Entity> targets) {
        if (caster.level().isClientSide() || !PowerLookup.holdsType(caster, CANONICAL)) return;
        Level level = caster.level();
        PowerLookup.forEach(caster, CANONICAL, Config.class, cfg -> {
            if (!cfg.filter().test(spell)) return;
            cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(caster, level)));
            if (cfg.bientityAction().isEmpty()) return;
            for (int i = 0, n = targets.size(); i < n; i++) {
                BiEntityCtx ctx = BiEntityCtx.of(caster, targets.get(i), level);
                if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(ctx)) continue;
                cfg.bientityAction().get().run(ctx);
            }
        });
    }
}
