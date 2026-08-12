package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.rope.RopeAnchor;
import dev.overgrown.apoli.rope.RopeEndpointSource;
import dev.overgrown.apoli.rope.RopeManager;
import dev.overgrown.apoli.rope.RopeParams;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class AttachRopeAction implements ActionType<EntityCtx, AttachRopeAction.Cfg> {
    public record Cfg(RopeEndpointSource from, RopeEndpointSource to, Optional<String> slot,
                      ResourceLocation texture, boolean toggle, RopeParams params) {}

    public static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        RopeEndpointSource.CODEC.codec().optionalFieldOf("from", RopeEndpointSource.self()).forGetter(Cfg::from),
        RopeEndpointSource.CODEC.codec().optionalFieldOf("to", RopeEndpointSource.raycastBlock(30f)).forGetter(Cfg::to),
        Codec.STRING.optionalFieldOf("slot").forGetter(Cfg::slot),
        IdCodecs.ID.optionalFieldOf("texture", Apoli.id("textures/rope/rope.png")).forGetter(Cfg::texture),
        Codec.BOOL.optionalFieldOf("toggle", true).forGetter(Cfg::toggle),
        RopeParams.CODEC.forGetter(Cfg::params)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() { return CODEC; }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        apply(cfg, ctx.raw(), null, ctx.level());
    }

    static void apply(Cfg cfg, @Nullable Entity actor, @Nullable Entity target, Level level) {
        if (actor == null || !(level instanceof ServerLevel serverLevel)) return;

        if (cfg.toggle && cfg.slot.isEmpty() && RopeManager.detachControllable(actor.getUUID())) return;

        RopeAnchor from = cfg.from.resolve(actor, target, level);
        RopeAnchor to = cfg.to.resolve(actor, target, level);
        if (from == null || to == null) return;

        RopeManager.create(serverLevel, from, to, cfg.slot.orElse(null), actor.getUUID(), cfg.params, cfg.texture);
    }

    public static final class BiEntity implements ActionType<BiEntityCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return CODEC;
        }

        @Override
        public void run(Cfg cfg, BiEntityCtx ctx) {
            apply(cfg, ctx.actor(), ctx.target(), ctx.level());
        }
    }
}
