package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.dev.DevMode;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PowerStoragePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StorePowerAction {

    public enum Operation implements StringRepresentable {
        ADD("add"), REMOVE("remove"), CLEAR("clear");

        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);

        private final String name;

        Operation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(Optional<ResourceLocation> storage, Optional<List<ResourceLocation>> powers,
                      List<String> tags, boolean fromHeld, Operation operation) {}

    private StorePowerAction() {}

    public static MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("storage", IdCodecs.ID).forGetter(Cfg::storage),
            LoggedOptionalField.strict("power", SingleOrList.of(IdCodecs.ID)).forGetter(Cfg::powers),
            LoggedOptionalField.of("tags", SingleOrList.of(Codec.STRING), List.of()).forGetter(Cfg::tags),
            Codec.BOOL.optionalFieldOf("from_held", false).forGetter(Cfg::fromHeld),
            Operation.CODEC.optionalFieldOf("operation", Operation.ADD).forGetter(Cfg::operation)
        ).apply(i, Cfg::new));
    }

    public static void apply(Cfg cfg, @Nullable Entity source, @Nullable Entity holder) {
        if (holder == null) return;
        boolean dev = DevMode.any();
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.powersOfType(ApoliIds.POWER_STORAGE).isEmpty()) {
            if (dev) DevMode.report(holder, "store_power: nothing here holds an apoli:power_storage power");
            return;
        }

        List<ResourceLocation> wanted = cfg.operation() == Operation.CLEAR
            ? List.of()
            : collect(cfg, source);
        if (wanted.isEmpty() && cfg.operation() != Operation.CLEAR) {
            if (dev) reportNothingCollected(cfg, source, holder);
            return;
        }

        boolean[] matched = new boolean[1];
        PowerLookup.forEachEntry(holder, ApoliIds.POWER_STORAGE, PowerStoragePower.Config.class, (storageId, storage) -> {
            if (cfg.storage().isPresent() && !cfg.storage().get().equals(storageId)) return;
            matched[0] = true;
            switch (cfg.operation()) {
                case CLEAR -> {
                    PowerStoragePower.clear(container, storageId);
                    if (dev) DevMode.report(holder, "store_power: cleared " + storageId);
                }
                case REMOVE -> {
                    for (int i = 0; i < wanted.size(); i++) {
                        boolean removed = PowerStoragePower.remove(container, storageId, wanted.get(i));
                        if (dev) {
                            DevMode.report(holder, "store_power: " + storageId + " " + wanted.get(i)
                                + (removed ? " removed" : " was not stored"));
                        }
                    }
                }
                case ADD -> {
                    for (int i = 0; i < wanted.size(); i++) {
                        ResourceLocation powerId = wanted.get(i);
                        PowerStoragePower.StoreResult result =
                            PowerStoragePower.attempt(container, storageId, storage, powerId);
                        if (dev) {
                            DevMode.report(holder, "store_power: " + storageId + " " + powerId + " "
                                + describe(result, storage) + origin(cfg, source));
                        }
                    }
                }
            }
        });

        if (dev && !matched[0]) {
            DevMode.report(holder, "store_power: no storage power here is called "
                + cfg.storage().map(ResourceLocation::toString).orElse("?")
                + " — a sub-power of an apoli:multiple is <power id>_<key>");
        }
    }

    private static String origin(Cfg cfg, @Nullable Entity source) {
        if (!cfg.fromHeld()) return "";
        return " (read from " + (source == null ? "nothing" : source.getName().getString()) + ")";
    }

    private static String describe(PowerStoragePower.StoreResult result, PowerStoragePower.Config storage) {
        return switch (result) {
            case STORED -> "stored";
            case NO_CONTAINER -> "refused: the holder has no power container";
            case UNKNOWN_POWER -> "refused: no power is loaded with that id";
            case IS_STORAGE -> "refused: a power storage cannot be stored inside a power storage";
            case NOT_IN_POWERS -> "refused: it is not listed in this storage's 'powers'";
            case NO_MATCHING_TAG -> "refused: it carries none of this storage's tags " + storage.tags();
            case ALREADY_STORED -> "refused: it is already in this storage";
            case FULL -> "refused: all " + storage.slots() + " slot(s) are full and replace_oldest is off";
        };
    }

    private static void reportNothingCollected(Cfg cfg, @Nullable Entity source, Entity holder) {
        if (!cfg.fromHeld()) {
            DevMode.report(holder, "store_power: neither 'power' nor 'tags' named anything to store");
            return;
        }
        if (source == null) {
            DevMode.report(holder, "store_power: from_held is set but there is no actor to take powers from");
            return;
        }
        PowerContainer from = PowerContainer.of(source);
        if (from == null || from.isEmpty()) {
            DevMode.report(source, "store_power: from_held found no powers at all on this entity");
            return;
        }
        DevMode.report(source, "store_power: from_held found no power on this entity matching"
            + (cfg.tags().isEmpty() ? "" : " tags " + cfg.tags())
            + (cfg.powers().isPresent() ? " power " + cfg.powers().get() : "")
            + " — from_held only takes powers this entity actually holds");
    }

    private static List<ResourceLocation> collect(Cfg cfg, @Nullable Entity source) {
        if (!cfg.fromHeld()) {
            List<ResourceLocation> explicit = cfg.powers().orElse(List.of());
            if (cfg.tags().isEmpty()) return explicit;
            List<ResourceLocation> out = new ArrayList<>(explicit);
            for (String tag : cfg.tags()) out.addAll(ApoliPowers.withTag(tag));
            return out;
        }
        PowerContainer from = source == null ? null : PowerContainer.of(source);
        if (from == null) return List.of();
        List<ResourceLocation> out = new ArrayList<>();
        for (ResourceLocation id : from.allPowers()) {
            if (cfg.powers().isPresent() && !cfg.powers().get().contains(id)) continue;
            if (!cfg.tags().isEmpty() && !ApoliPowers.hasAnyTag(id, cfg.tags())) continue;
            out.add(id);
        }
        return out;
    }

    public static final class BiEntity implements ActionType<BiEntityCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return configCodec();
        }

        @Override
        public void run(Cfg cfg, BiEntityCtx ctx) {
            apply(cfg, ctx.rawActor(), ctx.rawTarget());
        }
    }

    public static final class Self implements ActionType<EntityCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return configCodec();
        }

        @Override
        public void run(Cfg cfg, EntityCtx ctx) {
            apply(cfg, ctx.raw(), ctx.raw());
        }
    }
}
