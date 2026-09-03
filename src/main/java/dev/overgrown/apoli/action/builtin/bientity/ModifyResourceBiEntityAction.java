package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.builtin.entity.ModifyResourceAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.BiEntitySide;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.Optional;

public final class ModifyResourceBiEntityAction implements ActionType<BiEntityCtx, ModifyResourceBiEntityAction.Cfg> {

    public record Cfg(ModifyResourceAction.Cfg resource, BiEntitySide side, Optional<BiEntitySide> fromSide) {}

    @Override
    public MapCodec<Cfg> codec() {
        return dev.overgrown.apoli.alias.AliasingMapCodec.wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                ModifyResourceAction.CONFIG_CODEC.forGetter(Cfg::resource),
                BiEntitySide.CODEC.optionalFieldOf("side", BiEntitySide.TARGET).forGetter(Cfg::side),
                dev.overgrown.apoli.codec.LoggedOptionalField.strict("from_side", BiEntitySide.CODEC)
                    .forGetter(Cfg::fromSide)
            ).apply(i, Cfg::new)),
            Map.of("recipient", "side", "from_entity", "from_side"));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity recipient = cfg.side.of(ctx);
        if (recipient == null) return;
        BiEntitySide fromSide = cfg.fromSide.orElse(cfg.side.opposite());
        Entity source = fromSide.of(ctx);
        if (source == null) source = recipient;
        ModifyResourceAction.run(cfg.resource, recipient, source);
    }
}
