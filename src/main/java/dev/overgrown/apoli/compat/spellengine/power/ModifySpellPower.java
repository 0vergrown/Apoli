package dev.overgrown.apoli.compat.spellengine.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeUsage;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// Changes the cast time and cooldown (in seconds) and range (in blocks) of the holder's spells. Applied on top of what
// Spell Engine works out, after spell haste. The client needs it too, for the cast bar and targeting.
public final class ModifySpellPower extends PowerType<ModifySpellPower.Cfg> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_spell");
    private static final PowerTypeUsage.Handle HELD = PowerTypeUsage.handle(CANONICAL);

    public record Cfg(SpellFilter filter, List<AttributeModifier> castTime, List<AttributeModifier> cooldown,
                      List<AttributeModifier> range) {
        public Cfg {
            castTime = AttributeModifierHelper.ensureSorted(castTime);
            cooldown = AttributeModifierHelper.ensureSorted(cooldown);
            range = AttributeModifierHelper.ensureSorted(range);
        }
    }

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SpellFilter.MAP_CODEC.forGetter(Cfg::filter),
        AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("cast_time", List.of()).forGetter(Cfg::castTime),
        AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("cooldown", List.of()).forGetter(Cfg::cooldown),
        AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("range", List.of()).forGetter(Cfg::range)
    ).apply(i, Cfg::new));

    private static final Function<Cfg, List<AttributeModifier>> CAST_TIME = Cfg::castTime;
    private static final Function<Cfg, List<AttributeModifier>> COOLDOWN = Cfg::cooldown;
    private static final Function<Cfg, List<AttributeModifier>> RANGE = Cfg::range;

    @Override
    public MapCodec<Cfg> configCodec() {
        return CODEC;
    }

    @Override
    public boolean resolvesForClient() {
        return true;
    }

    // Spell Engine asks for these a lot (every frame on the client), so they return early unless the caster has the type.
    public static float castTime(@Nullable LivingEntity caster, @Nullable Spell spell, float value) {
        if (!holds(caster) || spell == null) return value;
        return apply(caster, SpellRegistry.from(caster.level()).wrapAsHolder(spell), value, CAST_TIME);
    }

    public static float cooldown(@Nullable LivingEntity caster, @Nullable Holder<Spell> spell, float value) {
        return holds(caster) && spell != null ? apply(caster, spell, value, COOLDOWN) : value;
    }

    public static float range(@Nullable LivingEntity caster, @Nullable Holder<Spell> spell, float value) {
        return holds(caster) && spell != null ? apply(caster, spell, value, RANGE) : value;
    }

    private static boolean holds(@Nullable LivingEntity caster) {
        if (caster == null || (!caster.level().isClientSide() && !HELD.isHeld())) return false;
        return PowerLookup.holdsType(caster, CANONICAL);
    }

    private static float apply(LivingEntity caster, Holder<Spell> spell, float value, Function<Cfg, List<AttributeModifier>> field) {
        List<AttributeModifier>[] mods = new List[1];
        boolean[] merged = {false};
        PowerLookup.forEach(caster, CANONICAL, Cfg.class, cfg -> {
            List<AttributeModifier> m = field.apply(cfg);
            if (m.isEmpty() || (!cfg.filter().isAny() && !cfg.filter().test(spell))) return;
            if (mods[0] == null) {
                mods[0] = m;
                return;
            }
            if (!merged[0]) mods[0] = new ArrayList<>(mods[0]);
            merged[0] = true;
            mods[0].addAll(m);
        });
        if (mods[0] == null) return value;
        List<AttributeModifier> list = merged[0] ? AttributeModifierHelper.ensureSorted(mods[0]) : mods[0];
        return (float) Math.max(0, AttributeModifierHelper.apply((double) value, list, caster));
    }
}
