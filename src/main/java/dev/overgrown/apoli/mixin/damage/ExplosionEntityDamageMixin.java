package dev.overgrown.apoli.mixin.damage;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.overgrown.apoli.access.ExplosionDamageToggle;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(Explosion.class)
public abstract class ExplosionEntityDamageMixin implements ExplosionDamageToggle {

    @Unique
    private boolean apoli$sparePassersby;

    @Unique
    @Nullable
    private Entity apoli$exempt;

    @Unique
    @Nullable
    private Predicate<Entity> apoli$damageFilter;

    @Override
    public void apoli$sparePassersby(@Nullable Entity exempt) {
        this.apoli$sparePassersby = true;
        this.apoli$exempt = exempt;
    }

    @Override
    public void apoli$setDamageFilter(@Nullable Predicate<Entity> filter) {
        this.apoli$damageFilter = filter;
    }

    @WrapWithCondition(method = "explode", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean apoli$skipExplosionDamage(Entity entity, DamageSource source, float amount) {
        if (this.apoli$sparePassersby && entity != this.apoli$exempt) return false;
        return this.apoli$damageFilter == null || this.apoli$damageFilter.test(entity);
    }
}
