package dev.overgrown.apoli.mixin.effects;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.effects.CustomEffect;
import dev.overgrown.apoli.effects.RuntimeMobEffectRegistry;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;


@Mixin(MappedRegistry.class)
public abstract class MobEffectRegistryCustomEffectMixin<T> implements RuntimeMobEffectRegistry {
    @Final
    @Shadow ResourceKey<Registry<?>> key;
    @Final
    @Shadow
    private ObjectList<Holder.Reference<T>> byId;
    @Final
    @Shadow
    private Reference2IntMap<T> toId;
    @Final
    @Shadow
    private Map<ResourceLocation, Holder.Reference<T>> byLocation;
    @Final
    @Shadow
    private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
    @Final
    @Shadow
    private Map<T, Holder.Reference<T>> byValue;
    @Final
    @Shadow
    private Map<ResourceKey<T>, RegistrationInfo> registrationInfos;

    @Unique
    private volatile boolean allowWrite = false;

    @Unique
    private int size = -1;

    @WrapMethod(method = "validateWrite()V")
    void apoli$allowRuntimeWrite(Operation<Void> original) {
        if (key.equals(Registries.MOB_EFFECT) && allowWrite) {
            Apoli.LOGGER.debug("Allowed Write to MOB_EFFECT Registry!");

            return;
        }

        original.call();
    }

    @WrapMethod(method = "validateWrite(Lnet/minecraft/resources/ResourceKey;)V")
    void apoli$allowRuntimeWriteKey(ResourceKey<T> resourceKey, Operation<Void> original) {
        if (key.equals(Registries.MOB_EFFECT) && allowWrite) {
            Apoli.LOGGER.debug("Allowed Write to MOB_EFFECT Registry! Key: {}", resourceKey);

            return;
        }

        original.call(resourceKey);
    }

    @Inject(method = "freeze", at = @At(value = "TAIL"))
    void apoli$onFreeze(CallbackInfoReturnable<Registry<T>> cir) {
        if (key.equals(Registries.MOB_EFFECT)) {
            size = ((MappedRegistry<?>) (Object) this).size();
        }
    }

    @Override
    public void apoli$truncate(List<CustomEffect> effects) {
        if (!key.equals(Registries.MOB_EFFECT)) {
            Apoli.LOGGER.warn("Method apoli$truncate called on non-MOB_EFFECT Registry.");

            return;
        }

        if (size == -1) {
            Apoli.LOGGER.warn("Registry hasn't been frozen yet. No changes where made!");

            return;
        }

        for (int i = size; i < byId.size(); i++) {
            Holder.Reference<T> h = byId.get(i);
            if (h == null) continue;
            byKey.remove(h.key());
            byLocation.remove(h.key().location());
            registrationInfos.remove(h.key());
            byValue.remove(h.value());
            toId.removeInt(h.value());
        }
        byId.size(size);
    }

    @Override
    public void apoli$register(CustomEffect effect) {
        if (!key.equals(Registries.MOB_EFFECT)) {
            Apoli.LOGGER.warn("Method apoli$register called on non-MOB_EFFECT Registry.");

            return;
        }

        allowWrite = true;
        try {
            Registry.register((Registry<T>) this, effect.id(), (T) effect.getMobEffect());
        } finally {
            allowWrite = false;
        }

    }
}
