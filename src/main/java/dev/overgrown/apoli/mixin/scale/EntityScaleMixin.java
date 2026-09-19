package dev.overgrown.apoli.mixin.scale;

import dev.overgrown.apoli.scale.ScaleHolder;
import dev.overgrown.apoli.scale.ScaleState;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void apoli$scaleDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        if (Scales.untouched(self)) return;
        float width = Scales.applied(self, ScaleTypes.HITBOX_WIDTH);
        float height = Scales.applied(self, ScaleTypes.HITBOX_HEIGHT);
        float eye = Scales.applied(self, ScaleTypes.EYE_HEIGHT);
        if (width == 1.0F && height == 1.0F && eye == 1.0F) return;
        EntityDimensions base = cir.getReturnValue();
        EntityDimensions scaled = base.scale(width, height);
        if (eye != height) scaled = scaled.withEyeHeight(base.eyeHeight() * eye);
        cir.setReturnValue(scaled);
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

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void apoli$saveScales(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        ScaleState state = this.apoli$scaleState;
        if (state != null && !state.isDefault()) state.save(cir.getReturnValue());
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
