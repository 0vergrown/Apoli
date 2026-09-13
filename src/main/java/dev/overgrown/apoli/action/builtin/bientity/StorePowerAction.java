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
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.powersOfType(ApoliIds.POWER_STORAGE).isEmpty()) return;

        List<ResourceLocation> wanted = cfg.operation() == Operation.CLEAR
            ? List.of()
            : collect(cfg, source);
        if (wanted.isEmpty() && cfg.operation() != Operation.CLEAR) return;

        PowerLookup.forEachEntry(holder, ApoliIds.POWER_STORAGE, PowerStoragePower.Config.class, (storageId, storage) -> {
            if (cfg.storage().isPresent() && !cfg.storage().get().equals(storageId)) return;
            switch (cfg.operation()) {
                case CLEAR -> PowerStoragePower.clear(container, storageId);
                case REMOVE -> {
                    for (int i = 0; i < wanted.size(); i++) {
                        PowerStoragePower.remove(container, storageId, wanted.get(i));
                    }
                }
                case ADD -> {
                    for (int i = 0; i < wanted.size(); i++) {
                        PowerStoragePower.store(container, storageId, storage, wanted.get(i));
                    }
                }
            }
        });
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
