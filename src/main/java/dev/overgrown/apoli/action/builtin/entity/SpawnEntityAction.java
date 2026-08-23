package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Nbt;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;

import java.util.Optional;

public final class SpawnEntityAction implements ActionType<EntityCtx, SpawnEntityAction.Cfg> {
    public record Cfg(
        ResourceLocation entityType,
        Optional<Nbt> tag,
        Optional<EntityAction> entityAction,
        Optional<BiEntityAction> bientityAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("entity_type").forGetter(Cfg::entityType),
            Nbt.CODEC.optionalFieldOf("tag").forGetter(Cfg::tag),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Cfg::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Cfg::bientityAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(cfg.entityType);
        if (type == null) return;
        Entity actor = ctx.entity();
        CompoundTag spawnTag = new CompoundTag();
        cfg.tag.ifPresent(n -> spawnTag.merge(n.tag()));
        spawnTag.putString("id", cfg.entityType.toString());
        Entity spawned = EntityType.loadEntityRecursive(spawnTag, level, e -> {
            e.moveTo(actor.getX(), actor.getY(), actor.getZ(), actor.getYRot(), actor.getXRot());
            return e;
        });
        if (spawned == null) return;
        if (spawned instanceof net.minecraft.world.entity.Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        }
        level.addFreshEntity(spawned);
        cfg.entityAction.ifPresent(a -> a.run(new EntityCtx(spawned, level)));
        cfg.bientityAction.ifPresent(a -> a.run(new BiEntityCtx(actor, spawned, level)));
    }
}
