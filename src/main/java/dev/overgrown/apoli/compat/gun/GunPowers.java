package dev.overgrown.apoli.compat.gun;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.gun.power.GunPowerType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GunPowers {
    private GunPowers() {}

    public static final ResourceLocation MODIFY_GUN_DAMAGE = Apoli.id("modify_gun_damage");
    public static final ResourceLocation MODIFY_GUN_DAMAGE_TAKEN = Apoli.id("modify_gun_damage_taken");
    public static final ResourceLocation MODIFY_HEAD_SHOT_DAMAGE = Apoli.id("modify_head_shot_damage");
    public static final ResourceLocation MODIFY_GUN_FIRE_RATE = Apoli.id("modify_gun_fire_rate");
    public static final ResourceLocation MODIFY_GUN_RECOIL = Apoli.id("modify_gun_recoil");
    public static final ResourceLocation PREVENT_AIM = Apoli.id("prevent_aim");
    public static final ResourceLocation PREVENT_GUN_FIRE = Apoli.id("prevent_gun_fire");
    public static final ResourceLocation ACTION_ON_GUN_FIRE = Apoli.id("action_on_gun_fire");
    public static final ResourceLocation ACTION_ON_GUN_BLOCK_HIT = Apoli.id("action_on_gun_block_hit");

    public static void registerPowers() {
        PowerTypeRegistry.register(MODIFY_GUN_DAMAGE, new GunPowerType<>(GunConfigs.Damage.CODEC));
        PowerTypeRegistry.register(MODIFY_GUN_DAMAGE_TAKEN, new GunPowerType<>(GunConfigs.Damage.CODEC));
        PowerTypeRegistry.register(MODIFY_HEAD_SHOT_DAMAGE, new GunPowerType<>(GunConfigs.Headshot.CODEC));
        PowerTypeRegistry.register(MODIFY_GUN_FIRE_RATE, new GunPowerType<>(GunConfigs.Stat.CODEC));
        PowerTypeRegistry.register(MODIFY_GUN_RECOIL, new GunPowerType<>(GunConfigs.Stat.CODEC));
        PowerTypeRegistry.register(PREVENT_AIM, new GunPowerType<>(GunFilter.MAP_CODEC));
        PowerTypeRegistry.register(PREVENT_GUN_FIRE, new GunPowerType<>(GunFilter.MAP_CODEC));
        PowerTypeRegistry.register(ACTION_ON_GUN_FIRE, new GunPowerType<>(GunConfigs.Fire.CODEC));
        PowerTypeRegistry.register(ACTION_ON_GUN_BLOCK_HIT, new GunPowerType<>(GunConfigs.BlockHit.CODEC));
    }

    public static float modifyDamage(@Nullable LivingEntity attacker, @Nullable Entity target, @Nullable ResourceLocation gun,
                                     boolean headshot, float base) {
        double value = base;
        if (attacker != null && PowerLookup.holdsType(attacker, MODIFY_GUN_DAMAGE)) {
            value = damage(attacker, MODIFY_GUN_DAMAGE, attacker, target, gun, headshot, value);
        }
        if (target instanceof LivingEntity victim && PowerLookup.holdsType(victim, MODIFY_GUN_DAMAGE_TAKEN)) {
            value = damage(victim, MODIFY_GUN_DAMAGE_TAKEN, attacker, target, gun, headshot, value);
        }
        return (float) value;
    }

    private static double damage(LivingEntity holder, ResourceLocation type, @Nullable Entity attacker, @Nullable Entity target,
                                 @Nullable ResourceLocation gun, boolean headshot, double value) {
        List<AttributeModifier> mods = new ArrayList<>(2);
        PowerLookup.forEach(holder, type, GunConfigs.Damage.class, cfg -> {
            if (!cfg.filter().test(gun)) return;
            if (cfg.headshot().isPresent() && cfg.headshot().get() != headshot) return;
            if (!bientity(cfg.bientityCondition().orElse(null), attacker, target, holder.level())) return;
            mods.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()));
        });
        return mods.isEmpty() ? value : AttributeModifierHelper.apply(value, AttributeModifierHelper.ensureSorted(mods), holder);
    }

    public static double modifyHeadshot(@Nullable LivingEntity attacker, @Nullable Entity target, @Nullable ResourceLocation gun,
                                        double multiplier) {
        if (attacker == null || !PowerLookup.holdsType(attacker, MODIFY_HEAD_SHOT_DAMAGE)) return multiplier;
        List<AttributeModifier> mods = new ArrayList<>(2);
        PowerLookup.forEach(attacker, MODIFY_HEAD_SHOT_DAMAGE, GunConfigs.Headshot.class, cfg -> {
            if (!cfg.filter().test(gun)) return;
            if (!bientity(cfg.bientityCondition().orElse(null), attacker, target, attacker.level())) return;
            mods.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()));
        });
        return mods.isEmpty() ? multiplier : AttributeModifierHelper.apply(multiplier, AttributeModifierHelper.ensureSorted(mods), attacker);
    }

    public static double modifyStat(@Nullable LivingEntity shooter, ResourceLocation type, @Nullable ResourceLocation gun, double value) {
        if (shooter == null || !PowerLookup.holdsType(shooter, type)) return value;
        List<AttributeModifier> mods = new ArrayList<>(2);
        PowerLookup.forEach(shooter, type, GunConfigs.Stat.class, cfg -> {
            if (cfg.filter().test(gun)) mods.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()));
        });
        return mods.isEmpty() ? value : AttributeModifierHelper.apply(value, AttributeModifierHelper.ensureSorted(mods), shooter);
    }

    public static boolean prevents(@Nullable LivingEntity entity, ResourceLocation type, @Nullable ResourceLocation gun) {
        return entity != null && PowerLookup.holdsType(entity, type)
            && PowerLookup.anyActive(entity, type, GunFilter.class, filter -> filter.test(gun));
    }

    public static void onFire(LivingEntity shooter, ItemStack gunStack, @Nullable ResourceLocation gun) {
        if (shooter.level().isClientSide() || !PowerLookup.holdsType(shooter, ACTION_ON_GUN_FIRE)) return;
        Level level = shooter.level();
        PowerLookup.forEach(shooter, ACTION_ON_GUN_FIRE, GunConfigs.Fire.class, cfg -> {
            if (!cfg.filter().test(gun)) return;
            cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(shooter, level)));
            cfg.itemAction().ifPresent(action -> action.run(new ItemCtx(gunStack, level, shooter)));
        });
    }

    public static void onBlockHit(@Nullable LivingEntity shooter, @Nullable ResourceLocation gun, Level level, BlockPos pos) {
        if (shooter == null || level.isClientSide() || !PowerLookup.holdsType(shooter, ACTION_ON_GUN_BLOCK_HIT)) return;
        BlockState state = level.getBlockState(pos);
        PowerLookup.forEach(shooter, ACTION_ON_GUN_BLOCK_HIT, GunConfigs.BlockHit.class, cfg -> {
            if (!cfg.filter().test(gun)) return;
            BlockCtx ctx = new BlockCtx(pos, state, level, shooter);
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
            cfg.blockAction().ifPresent(action -> action.run(ctx));
            cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(shooter, level)));
        });
    }

    private static boolean bientity(@Nullable dev.overgrown.apoli.condition.BiEntityCondition condition, @Nullable Entity attacker,
                                    @Nullable Entity target, Level level) {
        if (condition == null) return true;
        return attacker != null && target != null && condition.test(BiEntityCtx.of(attacker, target, level));
    }
}
