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
        Expression.INT_OR_EXPR.fieldOf("cooldown").forGetter(cfg -> cfg.max().orElse(Expression.constant(0))),
        HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Cfg::hudRender),
        Codec.BOOL.optionalFieldOf("persistent", true).forGetter(Cfg::persistent)
    ).apply(i, (cooldown, hud, persistent) -> new Cfg(
        Optional.of(Expression.constant(0)),
        Optional.of(cooldown),
        Optional.of(Expression.constant(0)),
        hud,
        true,
        false,
        Optional.empty(),
        Optional.empty(),
        persistent,
        1
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

    @Override
    public boolean isCooldown() {
        return true;
    }

}
