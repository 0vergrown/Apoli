package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.keybind.KeyDispatch;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PowerStoragePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;

public final class RunStoredPowerAction implements ActionType<EntityCtx, RunStoredPowerAction.Cfg> {

    public record Cfg(Optional<ResourceLocation> storage, Optional<Key> key, Optional<ResourceLocation> power,
                      List<String> tags, int index, boolean all) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("storage", IdCodecs.ID).forGetter(Cfg::storage),
            LoggedOptionalField.strict("key", Key.CODEC).forGetter(Cfg::key),
            LoggedOptionalField.strict("power", IdCodecs.ID).forGetter(Cfg::power),
            LoggedOptionalField.of("tags", SingleOrList.of(Codec.STRING), List.of()).forGetter(Cfg::tags),
            Codec.INT.optionalFieldOf("index", -1).forGetter(Cfg::index),
            Codec.BOOL.optionalFieldOf("all", false).forGetter(Cfg::all)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return;
        if (container.powersOfType(ApoliIds.POWER_STORAGE).isEmpty()) return;

        String key = cfg.key().map(Key::key).orElse(null);
        boolean[] done = new boolean[1];

        PowerLookup.forEachEntry(entity, ApoliIds.POWER_STORAGE, PowerStoragePower.Config.class, (storageId, storage) -> {
            if (done[0] && !cfg.all()) return;
            if (cfg.storage().isPresent() && !cfg.storage().get().equals(storageId)) return;

            List<ResourceLocation> powers = PowerStoragePower.live(container, storageId);
            if (powers.isEmpty()) return;

            if (cfg.index() >= 0) {
                if (cfg.index() >= powers.size()) return;
                if (fire(entity, powers.get(cfg.index()), cfg, key)) done[0] = true;
                return;
            }

            for (int i = 0; i < powers.size(); i++) {
                if (done[0] && !cfg.all()) return;
                if (fire(entity, powers.get(i), cfg, key)) done[0] = true;
            }
        });
    }

    private static boolean fire(Entity entity, ResourceLocation powerId, Cfg cfg, String key) {
        if (cfg.power().isPresent() && !cfg.power().get().equals(powerId)) return false;
        if (!cfg.tags().isEmpty() && !ApoliPowers.hasAnyTag(powerId, cfg.tags())) return false;
        return KeyDispatch.pressPower(entity, powerId, key);
    }
}
