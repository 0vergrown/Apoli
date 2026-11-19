package dev.overgrown.apoli.mixin.possession;

import dev.overgrown.apoli.access.PossessionPartner;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPossessionMixin implements PossessionPartner {

    @Unique
    @Nullable
    private Entity apoli$possessionHost;

    @Unique
    @Nullable
    private Entity apoli$possessor;

    @Override
    public @Nullable Entity apoli$possessionHost() {
        return apoli$possessionHost;
    }

    @Override
    public @Nullable Entity apoli$possessor() {
        return apoli$possessor;
    }

    @Override
    public void apoli$setPossessionHost(@Nullable Entity host) {
        apoli$possessionHost = host;
    }

    @Override
    public void apoli$setPossessor(@Nullable Entity possessor) {
        apoli$possessor = possessor;
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void apoli$skipPossessionPartner(Entity other, CallbackInfo ci) {
        if (other != null && (other == apoli$possessionHost || other == apoli$possessor)) ci.cancel();
    }
}
