package dev.overgrown.apoli.compat.spellengine.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;

import java.util.Optional;

public final class PreventSpellCastPower extends PowerType<PreventSpellCastPower.Cfg> {
    public static final ResourceLocation CANONICAL = Apoli.id("prevent_spell_cast");

    // message: shown above the hotbar when a cast is blocked.
    public record Cfg(SpellFilter filter, Optional<Component> message) {}

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SpellFilter.MAP_CODEC.forGetter(Cfg::filter),
        TextComponent.CODEC.optionalFieldOf("message").forGetter(Cfg::message)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> configCodec() {
        return CODEC;
    }

    public static boolean prevents(Player caster, Holder<Spell> spell) {
        if (!PowerLookup.holdsType(caster, CANONICAL)) return false;
        Optional<Component>[] message = new Optional[1];
        boolean blocked = PowerLookup.anyActive(caster, CANONICAL, Cfg.class, cfg -> {
            if (!cfg.filter().test(spell)) return false;
            message[0] = cfg.message();
            return true;
        });
        if (blocked && message[0] != null) message[0].ifPresent(m -> caster.displayClientMessage(m, true));
        return blocked;
    }
}
