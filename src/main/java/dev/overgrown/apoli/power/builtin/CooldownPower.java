package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.OptionalInt;

public final class CooldownPower extends ResourcePower {

    private static final MapCodec<Cfg> CFG_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Expression.INT_OR_EXPR.fieldOf("cooldown").forGetter(Cfg::max),
        HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Cfg::hudRender),
        Codec.BOOL.optionalFieldOf("persistent", true).forGetter(Cfg::persistent)
    ).apply(i, (cooldown, hud, persistent) -> new Cfg(
        Expression.constant(0),
        cooldown,
        Optional.of(Expression.constant(0)),
        hud,
        true,
        false,
        Optional.empty(),
        Optional.empty(),
        persistent
    )));

    @Override
    public MapCodec<Cfg> configCodec() {
        return CFG_CODEC;
    }

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        OptionalInt cur = readValue(holder, powerId);
        if (cur.isPresent() && cur.getAsInt() > 0) {
            writeValue(holder, powerId, cur.getAsInt() - 1);
            return;
        }
        super.tick(powerId, cfg, holder);
    }

    public static boolean trigger(PowerContainer holder, ResourceLocation powerId) {
        OptionalInt cur = readValue(holder, powerId);
        if (cur.isEmpty() || cur.getAsInt() > 0) return false;
        dev.overgrown.apoli.power.Power loaded = dev.overgrown.apoli.power.ApoliPowers.get(powerId);
        if (loaded == null) return false;
        dev.overgrown.apoli.power.PowerType<?> type = dev.overgrown.apoli.power.PowerTypeRegistry.get(loaded.typeId());
        if (!(type instanceof CooldownPower cp)) return false;
        if (!(loaded.config() instanceof Cfg cfg)) return false;
        int max = cp.currentMax(cfg, holder, powerId);
        writeValue(holder, powerId, max);
        return true;
    }
}
