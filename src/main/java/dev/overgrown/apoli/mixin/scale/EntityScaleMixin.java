package dev.overgrown.apoli.mixin.scale;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.scale.ScaleHolder;
import dev.overgrown.apoli.scale.ScaleState;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

@Mixin(Entity.class)
public abstract class EntityScaleMixin implements ScaleHolder {

    @Unique
    private ScaleState apoli$scaleState;

    @Override
    public @Nullable ScaleState apoli$scales() {
        return this.apoli$scaleState;
    }

    @Override
    public ScaleState apoli$scalesOrCreate() {
        ScaleState state = this.apoli$scaleState;
        if (state == null) {
            state = new ScaleState((Entity) (Object) this);
            this.apoli$scaleState = state;
        }
        return state;
    }

    @ModifyReturnValue(method = "getDimensions", at = @At("RETURN"))
    private EntityDimensions apoli$scaleDimensions(EntityDimensions original) {
        return Scales.scaledDimensions((Entity) (Object) this, original);
    }

    @ModifyReturnValue(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F",
            at = @At("RETURN"))
    private float apoli$scaleEyeHeight(float original) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity || Scales.untouched(self)) return original;
        float height = Scales.applied(self, ScaleTypes.HITBOX_HEIGHT);
        float eye = Scales.applied(self, ScaleTypes.EYE_HEIGHT);
        if (eye == height || height <= 0.0F) return original;
        return original * (eye / height);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void apoli$tickScales(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        ScaleState state = this.apoli$scaleState;
        if (state != null && state.needsTick()) {
            Scales.tick(self, state);
            return;
        }
        if (!self.level().isClientSide()) return;
        if (!Scales.anyPowerLoaded() || !dev.overgrown.apoli.power.builtin.ScalePower.hasAny(self)) return;
        Scales.refreshDimensions(self, apoli$scalesOrCreate());
    }

    @ModifyReturnValue(method = "saveWithoutId", at = @At("RETURN"))
    private CompoundTag apoli$saveScales(CompoundTag saved) {
        ScaleState state = this.apoli$scaleState;
        if (state != null && !state.isDefault()) state.save(saved);
        return saved;
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void apoli$loadScales(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("scales", 9)) return;
        apoli$scalesOrCreate().load(tag);
        Entity self = (Entity) (Object) this;
        Scales.refreshDimensions(self, this.apoli$scaleState);
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void apoli$restoreScales(Entity original, CallbackInfo ci) {
        ScaleState from = ((ScaleHolder) original).apoli$scales();
        if (from == null || from.isDefault()) return;
        apoli$scalesOrCreate().copyFrom(from);
        Scales.refreshDimensions((Entity) (Object) this, this.apoli$scaleState);
    }
}
