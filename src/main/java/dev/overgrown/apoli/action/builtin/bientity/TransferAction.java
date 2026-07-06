package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TransferAction implements ActionType<BiEntityCtx, TransferAction.Cfg> {
    public static final ResourceLocation DEFAULT_SOURCE = Apoli.id("transferred");

    public enum Mode implements StringRepresentable {
        STEAL("steal"),
        GIVE("give");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private final String name;
        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(
        Mode mode,
        boolean copy,
        Optional<List<ResourceLocation>> sources,
        ResourceLocation newSource,
        boolean preserveSource,
        Optional<EntityAction> actorAction,
        Optional<EntityAction> targetAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Mode.CODEC.optionalFieldOf("mode", Mode.STEAL).forGetter(Cfg::mode),
            Codec.BOOL.optionalFieldOf("copy", false).forGetter(Cfg::copy),
            ResourceLocation.CODEC.listOf().optionalFieldOf("sources").forGetter(Cfg::sources),
            ResourceLocation.CODEC.optionalFieldOf("new_source", DEFAULT_SOURCE).forGetter(Cfg::newSource),
            Codec.BOOL.optionalFieldOf("preserve_source", false).forGetter(Cfg::preserveSource),
            EntityAction.CODEC.optionalFieldOf("actor_action").forGetter(Cfg::actorAction),
            EntityAction.CODEC.optionalFieldOf("target_action").forGetter(Cfg::targetAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        LivingEntity donor = cfg.mode == Mode.STEAL ? ctx.target() : ctx.actor();
        LivingEntity recipient = cfg.mode == Mode.STEAL ? ctx.actor() : ctx.target();
        if (donor == recipient) return;

        PowerContainer donorContainer = PowerContainer.of(donor);
        PowerContainer recipientContainer = PowerContainer.of(recipient);
        if (donorContainer == null || recipientContainer == null) return;

        Map<ResourceLocation, Set<ResourceLocation>> toTransfer = new LinkedHashMap<>();
        for (ResourceLocation power : donorContainer.allPowers()) {
            if (ApoliPowers.isSubPower(power)) continue;
            for (ResourceLocation source : donorContainer.sourcesOf(power)) {
                if (source.equals(cfg.newSource)) continue;
                if (cfg.sources.isPresent() && !cfg.sources.get().contains(source)) continue;
                toTransfer.computeIfAbsent(power, k -> new LinkedHashSet<>()).add(source);
            }
        }
        if (toTransfer.isEmpty()) {
            runFollowUps(cfg, ctx);
            return;
        }

        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : toTransfer.entrySet()) {
            ResourceLocation power = entry.getKey();
            for (ResourceLocation source : entry.getValue()) {
                recipientContainer.addPower(power, cfg.preserveSource ? source : cfg.newSource);
                if (!cfg.copy) donorContainer.removePower(power, source);
            }
        }

        runFollowUps(cfg, ctx);
    }

    private static void runFollowUps(Cfg cfg, BiEntityCtx ctx) {
        cfg.actorAction.ifPresent(a -> a.run(ctx.asActor()));
        cfg.targetAction.ifPresent(a -> a.run(ctx.asTarget()));
    }
}
