package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.mixin.raid.RaidsAccessor;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.GameRules;

import java.util.Optional;

public final class StartRaidAction implements ActionType<EntityCtx, StartRaidAction.Cfg> {
    private static final Expression ONE = Expression.constant(1);
    private static final int VILLAGE_SEARCH_RADIUS = 64;

    public record Cfg(
        Expression omenLevel,
        Optional<EntityAction> successAction,
        Optional<EntityAction> failAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.INT_OR_EXPR.optionalFieldOf("omen_level", ONE).forGetter(Cfg::omenLevel),
            LoggedOptionalField.of("success_action", EntityAction.CODEC).forGetter(Cfg::successAction),
            LoggedOptionalField.of("fail_action", EntityAction.CODEC).forGetter(Cfg::failAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return;
        if (startOrExtend(level, entity, cfg.omenLevel)) {
            cfg.successAction.ifPresent(a -> a.run(ctx));
        } else {
            cfg.failAction.ifPresent(a -> a.run(ctx));
        }
    }

    private static boolean startOrExtend(ServerLevel level, Entity entity, Expression omenLevel) {
        if (entity.isSpectator()
            || level.getDifficulty() == Difficulty.PEACEFUL
            || level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)
            || !level.dimensionType().hasRaids()) return false;
        BlockPos pos = entity.blockPosition();
        if (!level.isVillage(pos)) return false;

        Raids raids = level.getRaids();
        RaidsAccessor access = (RaidsAccessor) raids;
        Raid raid = access.apoli$getOrCreateRaid(level, villageCenter(level, pos));
        int max = raid.getMaxBadOmenLevel();
        if (raid.isOver() || (raid.isStarted() && raid.getBadOmenLevel() >= max)) return false;

        if (!raid.isStarted()) access.apoli$raidMap().putIfAbsent(raid.getId(), raid);
        int omen = Mth.clamp(omenLevel.evalInt(entity), 1, max);
        raid.setBadOmenLevel(Math.min(raid.getBadOmenLevel() + omen, max));
        if (entity instanceof ServerPlayer player && !raid.hasFirstWaveSpawned()) {
            player.awardStat(Stats.RAID_TRIGGER);
            CriteriaTriggers.BAD_OMEN.trigger(player);
        }
        raids.setDirty();
        return true;
    }

    private static BlockPos villageCenter(ServerLevel level, BlockPos pos) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        int count = 0;
        for (PoiRecord record : level.getPoiManager()
            .getInRange(holder -> holder.is(PoiTypeTags.VILLAGE), pos, VILLAGE_SEARCH_RADIUS, PoiManager.Occupancy.IS_OCCUPIED)
            .toList()) {
            BlockPos poi = record.getPos();
            x += poi.getX();
            y += poi.getY();
            z += poi.getZ();
            count++;
        }
        if (count == 0) return pos;
        double scale = 1.0 / count;
        return BlockPos.containing(x * scale, y * scale, z * scale);
    }
}
