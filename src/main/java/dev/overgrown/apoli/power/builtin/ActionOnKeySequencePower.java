package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.FunctionalKey;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.keybind.HeldKeys;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class ActionOnKeySequencePower extends PowerType<ActionOnKeySequencePower.Config> {

    public record Config(
        Optional<EntityAction> successAction,
        Optional<EntityAction> failAction,
        int cooldown,
        int timeout,
        HudRender hudRender,
        List<FunctionalKey> keys,
        List<String> keySequence,
        int[] prefixFunction
    ) {}

    private static final class SeqState {
        int progress;
        int cooldown;
        int idle;
        boolean pending;
        boolean completedNow;
    }

    private static final class PlayerState {
        final Set<String> heldLast = new HashSet<>();
        final Map<ResourceLocation, SeqState> seqs = new HashMap<>();
        final List<ResourceLocation> ids = new ArrayList<>(4);
        final Map<ResourceLocation, Config> configs = new HashMap<>();
        final BiConsumer<ResourceLocation, Config> collector = (id, cfg) -> {
            ids.add(id);
            configs.put(id, cfg);
        };
        long lastTick = Long.MIN_VALUE;
    }

    private final Map<UUID, PlayerState> states = new HashMap<>();

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.optionalFieldOf("success_action").forGetter(Config::successAction),
            EntityAction.CODEC.optionalFieldOf("fail_action").forGetter(Config::failAction),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(Config::cooldown),
            Codec.INT.optionalFieldOf("timeout", 20).forGetter(Config::timeout),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender),
            Codec.list(FunctionalKey.CODEC).fieldOf("keys").forGetter(Config::keys),
            Codec.list(Codec.STRING).fieldOf("key_sequence").forGetter(Config::keySequence)
        ).apply(i, (successAction, failAction, cooldown, timeout, hudRender, keys, keySequence) ->
            new Config(successAction, failAction, cooldown, timeout, hudRender, keys, keySequence,
                computePrefix(keySequence))));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        PlayerState ps = states.get(holder.rawOwner().getUUID());
        if (ps != null) ps.seqs.remove(powerId);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        PlayerState ps = states.get(holder.rawOwner().getUUID());
        if (ps != null) ps.seqs.remove(powerId);
    }

    public void forget(UUID player) {
        states.remove(player);
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (!(owner instanceof ServerPlayer player)) return;
        if (!(owner.level() instanceof ServerLevel level)) return;

        PlayerState ps = states.computeIfAbsent(player.getUUID(), u -> new PlayerState());
        long now = level.getGameTime();
        if (ps.lastTick == now) return;
        ps.lastTick = now;
        process(player, level, ps);
    }

    private void process(ServerPlayer player, ServerLevel level, PlayerState ps) {
        ps.ids.clear();
        ps.configs.clear();
        PowerLookup.forEachEntry(player, ApoliIds.ACTION_ON_KEY_SEQUENCE, Config.class, ps.collector);
        if (ps.ids.isEmpty()) return;
        if (ps.ids.size() > 1) Collections.sort(ps.ids);

        EntityCtx ctx = new EntityCtx(player, level);
        Set<String> held = HeldKeys.serverHeldSet(player.getUUID());
        List<String> edges = edges(held, ps.heldLast);
        ps.heldLast.clear();
        ps.heldLast.addAll(held);

        for (int i = 0; i < ps.ids.size(); i++) {
            ResourceLocation id = ps.ids.get(i);
            Config cfg = ps.configs.get(id);
            SeqState st = ps.seqs.computeIfAbsent(id, k -> new SeqState());
            st.completedNow = false;

            if (st.cooldown > 0) {
                st.cooldown--;
                continue;
            }
            if (!conditionHolds(id, ctx)) continue;

            for (int k = 0; k < cfg.keys.size(); k++) {
                FunctionalKey fk = cfg.keys.get(k);
                String keyName = fk.key().key();
                boolean down = held.contains(keyName);
                boolean edge = edges != null && edges.contains(keyName);
                if (fk.key().continuous() ? down : edge) fk.action().ifPresent(a -> a.run(ctx));
            }

            if (cfg.keySequence.isEmpty()) continue;

            if (edges == null) {
                if (st.progress > 0 || st.pending) {
                    st.idle++;
                    if (cfg.timeout > 0 && st.idle >= cfg.timeout) {
                        st.progress = 0;
                        st.idle = 0;
                        if (st.pending) fire(player, id, cfg, st, ctx);
                    }
                }
                continue;
            }

            st.idle = 0;
            for (int e = 0; e < edges.size(); e++) {
                if (feed(edges.get(e), cfg, st, ctx)) {
                    st.pending = true;
                    st.completedNow = true;
                    break;
                }
            }
        }

        arbitrate(player, ps, ctx);
    }

    private void arbitrate(ServerPlayer player, PlayerState ps, EntityCtx ctx) {
        for (int i = 0; i < ps.ids.size(); i++) {
            SeqState st = ps.seqs.get(ps.ids.get(i));
            if (st == null || !st.pending) continue;
            ResourceLocation id = ps.ids.get(i);
            Config cfg = ps.configs.get(id);
            if (subsumedByLongerCompletion(ps, id, cfg)) {
                st.pending = false;
                st.idle = 0;
                continue;
            }
            if (blockedByViableExtension(ps, id, cfg)) continue;
            fire(player, id, cfg, st, ctx);
        }
    }

    private static boolean subsumedByLongerCompletion(PlayerState ps, ResourceLocation self, Config cfg) {
        for (int i = 0; i < ps.ids.size(); i++) {
            ResourceLocation other = ps.ids.get(i);
            if (other.equals(self)) continue;
            SeqState st = ps.seqs.get(other);
            if (st == null || !st.completedNow) continue;
            if (isProperInfix(cfg.keySequence, ps.configs.get(other).keySequence)) return true;
        }
        return false;
    }

    private static boolean blockedByViableExtension(PlayerState ps, ResourceLocation self, Config cfg) {
        int len = cfg.keySequence.size();
        for (int i = 0; i < ps.ids.size(); i++) {
            ResourceLocation other = ps.ids.get(i);
            if (other.equals(self)) continue;
            SeqState st = ps.seqs.get(other);
            if (st == null || st.cooldown > 0 || st.progress < len) continue;
            if (isProperInfix(cfg.keySequence, ps.configs.get(other).keySequence)) return true;
        }
        return false;
    }

    private static boolean isProperInfix(List<String> shorter, List<String> longer) {
        int n = shorter.size();
        int m = longer.size();
        if (n >= m) return false;
        outer:
        for (int start = 0; start <= m - n; start++) {
            for (int i = 0; i < n; i++) {
                if (!shorter.get(i).equals(longer.get(start + i))) continue outer;
            }
            return true;
        }
        return false;
    }

    private void fire(ServerPlayer player, ResourceLocation powerId, Config cfg, SeqState st, EntityCtx ctx) {
        st.pending = false;
        st.progress = 0;
        st.idle = 0;
        st.cooldown = Math.max(cfg.cooldown, 0);
        cfg.successAction.ifPresent(a -> a.run(ctx));
        ApoliNetwork.sendActivated(player, new PowerActivatedS2C(powerId, cfg.cooldown));
    }

    private static List<String> edges(Set<String> held, Set<String> heldLast) {
        if (held.isEmpty()) return null;
        List<String> out = null;
        for (String key : held) {
            if (heldLast.contains(key)) continue;
            if (out == null) out = new ArrayList<>(2);
            out.add(key);
        }
        if (out != null && out.size() > 1) Collections.sort(out);
        return out;
    }

    private boolean feed(String key, Config cfg, SeqState state, EntityCtx ctx) {
        List<String> seq = cfg.keySequence;
        int m = seq.size();
        int before = state.progress;
        int q = before;
        while (q > 0 && !seq.get(q).equals(key)) q = cfg.prefixFunction[q - 1];
        if (seq.get(q).equals(key)) q++;

        if (q < before) {
            cfg.failAction.ifPresent(a -> a.run(ctx));
        }
        if (q == m) {
            state.progress = 0;
            return true;
        }
        state.progress = q;
        return false;
    }

    private static boolean conditionHolds(ResourceLocation powerId, EntityCtx ctx) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || loaded.condition().isEmpty()) return true;
        return loaded.condition().get().test(ctx);
    }

    private static int[] computePrefix(List<String> pattern) {
        int m = pattern.size();
        int[] pi = new int[m];
        int k = 0;
        for (int i = 1; i < m; i++) {
            while (k > 0 && !pattern.get(i).equals(pattern.get(k))) k = pi[k - 1];
            if (pattern.get(i).equals(pattern.get(k))) k++;
            pi[i] = k;
        }
        return pi;
    }
}
