package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Vector;
import dev.overgrown.apoli.entity.ApoliEntities;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import dev.overgrown.apoli.entity.summon.Summons;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class SummonMinionAction implements ActionType<EntityCtx, SummonMinionAction.Cfg> {

    public record Cfg(
        Optional<ResourceLocation> texture,
        boolean followOwner,
        Optional<Vector> followOffset,
        float scale,
        boolean invulnerable,
        int maxLifeTicks,
        Optional<ResourceLocation> summonId,
        List<ResourceLocation> powers,
        Optional<BiEntityAction> bientityAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("texture").forGetter(Cfg::texture),
            Codec.BOOL.optionalFieldOf("follow_owner", false).forGetter(Cfg::followOwner),
            Vector.CODEC.optionalFieldOf("follow_offset").forGetter(Cfg::followOffset),
            Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(Cfg::scale),
            Codec.BOOL.optionalFieldOf("invulnerable", false).forGetter(Cfg::invulnerable),
            Codec.INT.optionalFieldOf("max_life_ticks", 1200).forGetter(Cfg::maxLifeTicks),
            IdCodecs.ID.optionalFieldOf("summon_id").forGetter(Cfg::summonId),
            IdCodecs.ID.listOf().optionalFieldOf("powers", List.of()).forGetter(Cfg::powers),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Cfg::bientityAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity owner = ctx.living();
        if (owner == null || !(ctx.level() instanceof ServerLevel level)) return;

        MinionEntity minion = ApoliEntities.minionType().create(level);
        if (minion == null) return;

        minion.setOwner(owner);
        cfg.texture.ifPresent(minion::setTexture);
        minion.setScale(cfg.scale);
        minion.setInvulnerable(cfg.invulnerable);
        minion.setMaxLifeTime(cfg.maxLifeTicks);
        cfg.summonId.ifPresent(minion::setSummonId);

        Vec3 pos = owner.position();
        if (cfg.followOffset.isPresent()) {
            Vector offset = cfg.followOffset.get();
            if (cfg.followOwner) minion.setFollowOffset(offset.x(), offset.y(), offset.z());
            pos = pos.add(offset.x(), offset.y(), offset.z());
        }
        minion.setFollowingOwner(cfg.followOwner);
        minion.moveTo(pos.x, pos.y, pos.z, owner.getYHeadRot(), owner.getXRot());

        level.addFreshEntity(minion);
        Summons.grantPowers(minion, cfg.powers);
        cfg.bientityAction.ifPresent(action -> action.run(new BiEntityCtx(owner, minion, level)));
    }
}
