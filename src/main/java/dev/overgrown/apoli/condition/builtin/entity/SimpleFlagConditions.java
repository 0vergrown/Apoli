package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

public final class SimpleFlagConditions {
    private SimpleFlagConditions() {}

    private record FlagCondition(Predicate<LivingEntity> check) implements ConditionType<EntityCtx, EmptyCfg> {
        @Override public MapCodec<EmptyCfg> codec() { return MapCodec.unit(EmptyCfg.INSTANCE); }
        @Override public boolean test(EmptyCfg cfg, EntityCtx ctx) { return check.test(ctx.entity()); }
    }

    public static ConditionType<EntityCtx, EmptyCfg> climbing()     { return new FlagCondition(e -> e.onClimbable()); }
    public static ConditionType<EntityCtx, EmptyCfg> collidedHoriz(){ return new FlagCondition(e -> e.horizontalCollision); }
    public static ConditionType<EntityCtx, EmptyCfg> creativeFly()  { return new FlagCondition(e -> e instanceof Player p && p.getAbilities().flying); }
    public static ConditionType<EntityCtx, EmptyCfg> daytime()      { return new FlagCondition(e -> e.level().isDay()); }
    public static ConditionType<EntityCtx, EmptyCfg> exists()       { return new FlagCondition(e -> e != null && !e.isRemoved()); }
    public static ConditionType<EntityCtx, EmptyCfg> exposedToSky() { return new FlagCondition(e -> e.level().canSeeSky(e.blockPosition())); }
    public static ConditionType<EntityCtx, EmptyCfg> exposedToSun() {
        return new FlagCondition(e -> {
            if (!e.level().canSeeSky(e.blockPosition())) return false;
            return e.level().getBrightness(net.minecraft.world.level.LightLayer.SKY, e.blockPosition()) > 0;
        });
    }
    public static ConditionType<EntityCtx, EmptyCfg> fallFlying()   { return new FlagCondition(LivingEntity::isFallFlying); }
    public static ConditionType<EntityCtx, EmptyCfg> glowing()      {
        return new FlagCondition(e -> e.isCurrentlyGlowing() || PowerLookup.hasActive(e, Apoli.id("entity_glow")));
    }
    public static ConditionType<EntityCtx, EmptyCfg> inRain()       { return new FlagCondition(e -> e.level().isRainingAt(e.blockPosition())); }
    public static ConditionType<EntityCtx, EmptyCfg> inSnow()       {
        return new FlagCondition(e -> {
            net.minecraft.world.level.Level level = e.level();
            net.minecraft.core.BlockPos pos = e.blockPosition();
            return level.isRaining() && level.getBiome(pos).value().coldEnoughToSnow(pos);
        });
    }
    public static ConditionType<EntityCtx, EmptyCfg> inThunder()    {
        return new FlagCondition(e -> e.level().isThundering() && e.level().isRainingAt(e.blockPosition()));
    }
    public static ConditionType<EntityCtx, EmptyCfg> onFire()       { return new FlagCondition(LivingEntity::isOnFire); }
    public static ConditionType<EntityCtx, EmptyCfg> sprinting()    { return new FlagCondition(LivingEntity::isSprinting); }
    public static ConditionType<EntityCtx, EmptyCfg> swimming()     { return new FlagCondition(LivingEntity::isSwimming); }
    public static ConditionType<EntityCtx, EmptyCfg> tamed()        { return new FlagCondition(e -> e instanceof TamableAnimal t && t.isTame()); }
    public static ConditionType<EntityCtx, EmptyCfg> usingEffectiveTool() {
        return new FlagCondition(e -> {
            if (!(e instanceof Player player)) return false;
            net.minecraft.world.phys.HitResult hit = player.pick(4.5, 1f, false);
            if (!(hit instanceof net.minecraft.world.phys.BlockHitResult bhr)
                || bhr.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return false;
            return player.hasCorrectToolForDrops(player.level().getBlockState(bhr.getBlockPos()));
        });
    }

    @SuppressWarnings("unused")
    private static void keepImports(Abilities a) {}
}
