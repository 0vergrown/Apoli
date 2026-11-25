package dev.overgrown.apoli.compat.spellengine.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;

public final class BypassSpellRequirementsPower extends PowerType<BypassSpellRequirementsPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("bypass_spell_requirements");

    public record Config(SpellFilter filter, boolean cooldown, boolean items) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            SpellFilter.MAP_CODEC.forGetter(Config::filter),
            Codec.BOOL.optionalFieldOf("cooldown", true).forGetter(Config::cooldown),
            Codec.BOOL.optionalFieldOf("items", true).forGetter(Config::items)
        ).apply(i, Config::new));
    }

    public static boolean bypassesCooldown(Player player, Holder<Spell> spell) {
        return PowerLookup.holdsType(player, CANONICAL)
            && PowerLookup.anyActive(player, CANONICAL, Config.class, cfg -> cfg.cooldown() && cfg.filter().test(spell));
    }

    public static boolean bypassesItems(Player player, Holder<Spell> spell) {
        return PowerLookup.holdsType(player, CANONICAL)
            && PowerLookup.anyActive(player, CANONICAL, Config.class, cfg -> cfg.items() && cfg.filter().test(spell));
    }
}
