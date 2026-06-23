package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class RelativeAction implements ActionType<EntityCtx, RelativeAction.Cfg> {
    public enum Target implements StringRepresentable {
        PASSENGER("passenger"),
        VEHICLE("vehicle");

        public static final Codec<Target> CODEC = StringRepresentable.fromEnum(Target::values);
        private final String name;
        Target(String n) {
            this.name = n;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(
        Target target,
        Optional<EntityAction> action,
        Optional<BiEntityAction> bientityAction,
        Optional<BiEntityCondition> bientityCondition,
        boolean recursive
    ) {}

    private final Target defaultTarget;

    public RelativeAction(Target defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    public RelativeAction() {
        this(Target.PASSENGER);
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Target.CODEC.optionalFieldOf("target", defaultTarget).forGetter(Cfg::target),
            EntityAction.CODEC.optionalFieldOf("action").forGetter(Cfg::action),
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Cfg::bientityAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Cfg::bientityCondition),
            Codec.BOOL.optionalFieldOf("recursive", false).forGetter(Cfg::recursive)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity self = ctx.entity();
        Level level = ctx.level();
        if (cfg.target == Target.PASSENGER) {
            if (cfg.recursive) {
                for (Entity p : self.getIndirectPassengers()) fire(cfg, self, p, level);
            } else {
                for (Entity p : self.getPassengers()) fire(cfg, self, p, level);
            }
        } else {
            Entity vehicle = self.getVehicle();
            while (vehicle != null) {
                fire(cfg, self, vehicle, level);
                if (!cfg.recursive) break;
                vehicle = vehicle.getVehicle();
            }
        }
    }

    private static void fire(Cfg cfg, LivingEntity actor, Entity related, Level level) {
        if (!(related instanceof LivingEntity targetLiving)) return;
        if (cfg.bientityCondition.isPresent()
            && !cfg.bientityCondition.get().test(new BiEntityCtx(actor, targetLiving, level))) return;
        cfg.action.ifPresent(a -> a.run(new EntityCtx(targetLiving, level)));
        cfg.bientityAction.ifPresent(a -> a.run(new BiEntityCtx(actor, targetLiving, level)));
    }
}
