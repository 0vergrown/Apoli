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
import dev.overgrown.apoli.dev.DevMode;
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

    private static final int STORAGES = 0;
    private static final int CANDIDATES = 1;
    private static final int FIRED = 2;

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
        boolean dev = DevMode.any();
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()
            || container.powersOfType(ApoliIds.POWER_STORAGE).isEmpty()) {
            if (dev) DevMode.report(entity, "run_stored_power: nothing here holds an apoli:power_storage power");
            return;
        }

        String key = cfg.key().map(Key::key).orElse(null);
        boolean[] done = new boolean[1];
        int[] stats = dev ? new int[3] : null;

        PowerLookup.forEachEntry(entity, ApoliIds.POWER_STORAGE, PowerStoragePower.Config.class, (storageId, storage) -> {
            if (done[0] && !cfg.all()) return;
            if (cfg.storage().isPresent() && !cfg.storage().get().equals(storageId)) return;
            if (stats != null) stats[STORAGES]++;

            List<ResourceLocation> powers = PowerStoragePower.live(container, storageId);
            if (powers.isEmpty()) return;

            if (cfg.index() >= 0) {
                if (cfg.index() >= powers.size()) return;
                fire(entity, powers.get(cfg.index()), cfg, key, done, stats);
                return;
            }

            for (int i = 0; i < powers.size(); i++) {
                if (done[0] && !cfg.all()) return;
                fire(entity, powers.get(i), cfg, key, done, stats);
            }
        });

        if (stats != null && stats[FIRED] == 0) report(entity, cfg, stats);
    }

    private static void fire(Entity entity, ResourceLocation powerId, Cfg cfg, String key,
                             boolean[] done, int[] stats) {
        if (cfg.power().isPresent() && !cfg.power().get().equals(powerId)) return;
        if (!cfg.tags().isEmpty() && !ApoliPowers.hasAnyTag(powerId, cfg.tags())) return;
        if (stats != null) stats[CANDIDATES]++;
        if (!KeyDispatch.pressPower(entity, powerId, key)) return;
        if (stats != null) stats[FIRED]++;
        done[0] = true;
    }

    private static void report(Entity entity, Cfg cfg, int[] stats) {
        if (stats[STORAGES] == 0) {
            DevMode.report(entity, "run_stored_power: no storage power here is called "
                + cfg.storage().map(ResourceLocation::toString).orElse("?")
                + " — a sub-power of an apoli:multiple is <power id>_<key>");
            return;
        }
        if (stats[CANDIDATES] == 0) {
            DevMode.report(entity, "run_stored_power: the storage holds nothing that matches"
                + (cfg.power().isPresent() ? " power " + cfg.power().get() : "")
                + (cfg.tags().isEmpty() ? "" : " tags " + cfg.tags())
                + (cfg.index() >= 0 ? " index " + cfg.index() : ""));
            return;
        }
        DevMode.report(entity, "run_stored_power: " + stats[CANDIDATES]
            + " stored power(s) matched but none fired — a stored power only answers this if it is"
            + " key-driven (action_on_key_press, toggle, fire_projectile, inventory or a multiple of"
            + " those), and its own condition, cooldown and prevent_key_press still apply");
    }
}
