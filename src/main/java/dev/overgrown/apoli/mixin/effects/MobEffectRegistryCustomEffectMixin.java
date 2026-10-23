package dev.overgrown.apoli.mixin.effects;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.overgrown.apoli.effects.CustomMobEffect;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Mixin(MappedRegistry.class)
public abstract class MobEffectRegistryCustomEffectMixin<T> implements RuntimeMobEffectRegistry {
    @Final
    @Shadow
    private ResourceKey<? extends Registry<T>> key;
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
    private boolean apoli$allowWrite;

    @Unique
    private Map<ResourceKey<T>, Holder.Reference<T>> apoli$retired;

    @WrapMethod(method = "validateWrite()V")
    private void apoli$allowRuntimeWrite(Operation<Void> original) {
        if (this.apoli$allowWrite) return;
        original.call();
    }

    @WrapMethod(method = "validateWrite(Lnet/minecraft/resources/ResourceKey;)V")
    private void apoli$allowRuntimeWriteKey(ResourceKey<T> resourceKey, Operation<Void> original) {
        if (this.apoli$allowWrite) return;
        original.call(resourceKey);
    }

    @Override
    public void apoli$unregisterCustom(Set<ResourceLocation> keep) {
        if (!this.key.equals(Registries.MOB_EFFECT)) return;

        Iterator<Map.Entry<ResourceKey<T>, Holder.Reference<T>>> keys = this.byKey.entrySet().iterator();
        while (keys.hasNext()) {
            Map.Entry<ResourceKey<T>, Holder.Reference<T>> entry = keys.next();
            if (!apoli$retires(entry.getValue(), keep)) continue;
            ResourceKey<T> retiredKey = entry.getKey();
            Holder.Reference<T> retiredReference = entry.getValue();
            keys.remove();
            this.registrationInfos.remove(retiredKey);
            if (this.apoli$retired == null) this.apoli$retired = new HashMap<>();
            this.apoli$retired.put(retiredKey, retiredReference);
        }

        Iterator<Holder.Reference<T>> locations = this.byLocation.values().iterator();
        while (locations.hasNext()) {
            if (apoli$retires(locations.next(), keep)) locations.remove();
        }

        Iterator<T> values = this.byValue.keySet().iterator();
        while (values.hasNext()) {
            if (apoli$retires(values.next(), keep)) values.remove();
        }

        Iterator<T> numbered = this.toId.keySet().iterator();
        while (numbered.hasNext()) {
            if (apoli$retires(numbered.next(), keep)) numbered.remove();
        }

        int first = -1;
        for (int i = 0; i < this.byId.size(); i++) {
            if (apoli$retires(this.byId.get(i), keep)) {
                first = i;
                break;
            }
        }
        if (first < 0) return;

        int write = first;
        for (int read = first; read < this.byId.size(); read++) {
            Holder.Reference<T> reference = this.byId.get(read);
            if (reference == null || apoli$retires(reference, keep)) continue;
            this.byId.set(write, reference);
            this.toId.put(reference.value(), write);
            write++;
        }
        this.byId.size(write);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CustomMobEffect apoli$register(CustomMobEffect effect) {
        if (!this.key.equals(Registries.MOB_EFFECT)) return effect;

        ResourceKey<T> resourceKey = ResourceKey.create(this.key, effect.id);
        CustomMobEffect installed = effect;
        if (this.apoli$retired != null) {
            Holder.Reference<T> previous = this.apoli$retired.remove(resourceKey);
            if (previous != null) {
                this.byKey.put(resourceKey, previous);
                if (previous.isBound() && previous.value() instanceof CustomMobEffect earlier) {
                    earlier.copyFrom(effect);
                    installed = earlier;
                }
            }
        }

        this.apoli$allowWrite = true;
        try {
            Registry.register((Registry<T>) (Object) this, resourceKey, (T) installed);
        } finally {
            this.apoli$allowWrite = false;
        }
        return installed;
    }

    @Unique
    private static boolean apoli$retires(Holder.Reference<?> reference, Set<ResourceLocation> keep) {
        return reference != null && reference.isBound() && apoli$retires(reference.value(), keep);
    }

    @Unique
    private static boolean apoli$retires(Object value, Set<ResourceLocation> keep) {
        return value instanceof CustomMobEffect effect && !keep.contains(effect.id);
    }
}
