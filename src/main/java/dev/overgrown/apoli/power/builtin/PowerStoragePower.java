package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class PowerStoragePower extends PowerType<PowerStoragePower.Config> {

    private static final String KEY = "Powers";

    public record Config(int slots, Optional<List<ResourceLocation>> powers, List<String> tags,
                         boolean replaceOldest, boolean dropOnDeath, boolean grant) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("slots", 0).forGetter(Config::slots),
            LoggedOptionalField.strict("powers", SingleOrList.of(IdCodecs.ID)).forGetter(Config::powers),
            LoggedOptionalField.of("tags", SingleOrList.of(Codec.STRING), List.of()).forGetter(Config::tags),
            Codec.BOOL.optionalFieldOf("replace_oldest", false).forGetter(Config::replaceOldest),
            Codec.BOOL.optionalFieldOf("drop_on_death", false).forGetter(Config::dropOnDeath),
            Codec.BOOL.optionalFieldOf("grant", true).forGetter(Config::grant)
        ).apply(i, Config::new));
    }

    public static List<ResourceLocation> stored(@Nullable PowerContainer holder, ResourceLocation storageId) {
        if (!(holder instanceof PowerContainerImpl impl)) return List.of();
        CompoundTag tag = impl.getAuxNbt(storageId);
        if (tag == null) return List.of();
        ListTag list = tag.getList(KEY, Tag.TAG_STRING);
        if (list.isEmpty()) return List.of();
        List<ResourceLocation> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) out.add(id);
        }
        return out;
    }

    public static List<ResourceLocation> live(@Nullable PowerContainer holder, ResourceLocation storageId) {
        List<ResourceLocation> all = stored(holder, storageId);
        if (all.isEmpty()) return all;
        List<ResourceLocation> out = null;
        for (int i = 0; i < all.size(); i++) {
            ResourceLocation id = all.get(i);
            if (ApoliPowers.get(id) != null) {
                if (out != null) out.add(id);
                continue;
            }
            if (out == null) {
                out = new ArrayList<>(all.size() - 1);
                for (int j = 0; j < i; j++) out.add(all.get(j));
            }
        }
        return out == null ? all : out;
    }

    public static int count(@Nullable PowerContainer holder, ResourceLocation storageId) {
        return live(holder, storageId).size();
    }

    public static boolean accepts(Config cfg, ResourceLocation powerId) {
        if (isStorage(powerId)) return false;
        if (cfg.powers().isPresent() && !cfg.powers().get().contains(powerId)) return false;
        if (cfg.tags().isEmpty()) return true;
        return ApoliPowers.hasAnyTag(powerId, cfg.tags());
    }

    public static boolean isStorage(ResourceLocation powerId) {
        Power power = ApoliPowers.get(powerId);
        return power != null && ApoliIds.POWER_STORAGE.equals(power.typeId());
    }

    public static boolean store(@Nullable PowerContainer holder, ResourceLocation storageId, Config cfg,
                                ResourceLocation powerId) {
        if (!(holder instanceof PowerContainerImpl impl)) return false;
        if (ApoliPowers.get(powerId) == null || !accepts(cfg, powerId)) return false;
        List<ResourceLocation> current = new ArrayList<>(stored(impl, storageId));
        current.removeIf(id -> ApoliPowers.get(id) == null);
        if (current.contains(powerId)) return false;
        if (cfg.slots() > 0 && current.size() >= cfg.slots()) {
            if (!cfg.replaceOldest()) return false;
            current.remove(0);
        }
        current.add(powerId);
        write(impl, storageId, current);
        return true;
    }

    public static boolean remove(@Nullable PowerContainer holder, ResourceLocation storageId,
                                 ResourceLocation powerId) {
        if (!(holder instanceof PowerContainerImpl impl)) return false;
        List<ResourceLocation> current = new ArrayList<>(stored(impl, storageId));
        if (!current.remove(powerId)) return false;
        write(impl, storageId, current);
        return true;
    }

    public static void replace(@Nullable PowerContainer holder, ResourceLocation storageId,
                               List<ResourceLocation> powers) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (powers.isEmpty()) {
            clear(impl, storageId);
            return;
        }
        write(impl, storageId, powers);
    }

    public static void clear(@Nullable PowerContainer holder, ResourceLocation storageId) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (impl.getAuxNbt(storageId) == null) return;
        impl.removeAux(storageId);
        sync(impl, storageId);
    }

    public static void sync(@Nullable PowerContainer holder, ResourceLocation storageId) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        Power storage = ApoliPowers.get(storageId);
        boolean grant = storage != null && storage.config() instanceof Config cfg && cfg.grant();
        List<ResourceLocation> wanted = grant ? live(impl, storageId) : List.of();
        for (ResourceLocation held : List.copyOf(impl.allPowers())) {
            if (wanted.contains(held)) continue;
            if (impl.sourcesOf(held).contains(storageId)) impl.removePower(held, storageId);
        }
        for (int i = 0; i < wanted.size(); i++) impl.addPower(wanted.get(i), storageId);
    }

    public static void reconcile(PowerContainer holder) {
        List<ResourceLocation> storages = storagesOf(holder);
        for (int i = 0; i < storages.size(); i++) sync(holder, storages.get(i));
    }

    private static void write(PowerContainerImpl impl, ResourceLocation storageId, List<ResourceLocation> powers) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < powers.size(); i++) list.add(StringTag.valueOf(powers.get(i).toString()));
        tag.put(KEY, list);
        impl.setAuxNbt(storageId, tag);
        impl.markDirty();
        sync(impl, storageId);
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (cfg.grant()) return;
        List<ResourceLocation> powers = stored(holder, powerId);
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation id = powers.get(i);
            Power power = ApoliPowers.get(id);
            if (power == null) continue;
            PowerType<?> type = PowerTypeRegistry.get(power.typeId());
            if (type == null) continue;
            invokeTickStored(type, id, power.config(), holder);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void invokeTickStored(PowerType type, ResourceLocation id, Object cfg, PowerContainer holder) {
        type.tickStored(id, cfg, holder);
    }

    @Override
    public boolean ticksNonLivingEntities() {
        return true;
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        if (holder instanceof PowerContainerImpl impl) impl.removeAux(powerId);
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        return OptionalInt.of(count(holder, powerId));
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Config cfg, PowerContainer holder, int value) {
        if (!(holder instanceof PowerContainerImpl impl)) return OptionalInt.empty();
        List<ResourceLocation> current = live(impl, powerId);
        int target = Math.max(0, Math.min(value, current.size()));
        if (target == 0) {
            clear(impl, powerId);
            return OptionalInt.of(0);
        }
        replace(impl, powerId, current.subList(current.size() - target, current.size()));
        return OptionalInt.of(target);
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Config cfg, PowerContainer holder, boolean max) {
        if (!max) return OptionalInt.of(0);
        return cfg.slots() > 0 ? OptionalInt.of(cfg.slots()) : OptionalInt.empty();
    }

    public static List<ResourceLocation> storagesOf(@Nullable PowerContainer holder) {
        return holder == null ? List.of() : holder.powersOfType(ApoliIds.POWER_STORAGE);
    }
}
