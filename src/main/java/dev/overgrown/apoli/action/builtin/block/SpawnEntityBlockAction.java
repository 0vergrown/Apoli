package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Nbt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class SpawnEntityBlockAction implements ActionType<BlockCtx, SpawnEntityBlockAction.Cfg> {
    public record Cfg(ResourceLocation entityType, Optional<Nbt> tag, Optional<EntityAction> entityAction) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("entity_type").forGetter(Cfg::entityType),
            Nbt.CODEC.optionalFieldOf("tag").forGetter(Cfg::tag),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Cfg::entityAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(cfg.entityType);
        if (type == null) return;
        Vec3 pos = Vec3.atBottomCenterOf(ctx.pos());
        CompoundTag tag = cfg.tag.map(n -> n.tag().copy()).orElseGet(CompoundTag::new);
        tag.putString("id", cfg.entityType.toString());
        Entity entity = EntityType.loadEntityRecursive(tag, serverLevel, loaded -> {
            loaded.moveTo(pos.x, pos.y, pos.z, loaded.getYRot(), loaded.getXRot());
            return loaded;
        });
        if (entity == null) return;
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(ctx.pos()),
                MobSpawnType.MOB_SUMMONED, null, null);
        }
        serverLevel.addFreshEntity(entity);
        if (cfg.entityAction.isPresent() && entity instanceof LivingEntity living) {
            cfg.entityAction.get().run(new EntityCtx(living, serverLevel));
        }
    }
}
