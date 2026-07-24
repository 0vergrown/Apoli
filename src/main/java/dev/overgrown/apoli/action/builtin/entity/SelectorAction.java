package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public final class SelectorAction implements ActionType<EntityCtx, SelectorAction.Cfg> {
    public record Cfg(
        String selector,
        Optional<BiEntityAction> bientityAction,
        Optional<BiEntityCondition> bientityCondition
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("selector").forGetter(Cfg::selector),
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Cfg::bientityAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Cfg::bientityCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity actor = ctx.entity();
        List<? extends Entity> targets;
        try {
            EntitySelector parsed = new EntitySelectorParser(new StringReader(cfg.selector), true).parse();
            targets = parsed.findEntities(actor.createCommandSourceStack().withPermission(4));
        } catch (Exception e) {
            return;
        }
        for (Entity target : targets) {
            if (cfg.bientityCondition.isPresent()
                && !cfg.bientityCondition.get().test(new BiEntityCtx(actor, target, ctx.level()))) continue;
            cfg.bientityAction.ifPresent(a -> a.run(new BiEntityCtx(actor, target, ctx.level())));
        }
    }
}
