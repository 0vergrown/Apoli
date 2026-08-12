package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class AdvancementAction implements ActionType<EntityCtx, AdvancementAction.Cfg> {
    public enum Selection implements StringRepresentable {
        ONLY("only"),
        THROUGH("through"),
        FROM("from"),
        UNTIL("until"),
        EVERYTHING("everything");

        public static final Codec<Selection> CODEC = StringRepresentable.fromEnum(Selection::values);
        private final String name;
        Selection(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public record Cfg(
        Optional<ResourceLocation> advancement,
        Optional<List<String>> criteria,
        Optional<String> criterion,
        Selection selection,
        boolean revoke
    ) {}

    private final boolean defaultRevoke;

    public AdvancementAction(boolean defaultRevoke) {
        this.defaultRevoke = defaultRevoke;
    }

    public AdvancementAction() {
        this(false);
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("advancement").forGetter(Cfg::advancement),
            Codec.list(Codec.STRING).optionalFieldOf("criteria").forGetter(Cfg::criteria),
            Codec.STRING.optionalFieldOf("criterion").forGetter(Cfg::criterion),
            Selection.CODEC.optionalFieldOf("selection", Selection.ONLY).forGetter(Cfg::selection),
            Codec.BOOL.optionalFieldOf("revoke", defaultRevoke).forGetter(Cfg::revoke)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        if (cfg.advancement.isEmpty()) return;
        ServerAdvancementManager mgr = player.server.getAdvancements();
        Advancement adv = mgr.getAdvancement(cfg.advancement.get());
        if (adv == null) return;
        PlayerAdvancements progress = player.getAdvancements();
        Set<String> criteria = resolveCriteria(cfg, adv);
        for (String c : criteria) {
            if (cfg.revoke) progress.revoke(adv, c);
            else progress.award(adv, c);
        }
    }

    private static Set<String> resolveCriteria(Cfg cfg, Advancement adv) {
        if (cfg.criteria.isPresent()) return Set.copyOf(cfg.criteria.get());
        if (cfg.criterion.isPresent()) return Set.of(cfg.criterion.get());
        return adv.getCriteria().keySet();
    }

}
