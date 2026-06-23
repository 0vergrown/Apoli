package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.network.VelocityUpdater;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class AddVelocityAction implements ActionType<EntityCtx, AddVelocityAction.Cfg> {
    public record Cfg(double x, double y, double z, Space space, boolean set) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(Cfg::x),
            Codec.DOUBLE.optionalFieldOf("y", 0.0).forGetter(Cfg::y),
            Codec.DOUBLE.optionalFieldOf("z", 0.0).forGetter(Cfg::z),
            Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(Cfg::space),
            Codec.BOOL.optionalFieldOf("set", false).forGetter(Cfg::set)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity recipient = ctx.entity();
        Vec3 delta = cfg.space.toGlobal(recipient, new Vec3(cfg.x, cfg.y, cfg.z));
        VelocityUpdater.apply(recipient, delta, cfg.set);
    }
}
