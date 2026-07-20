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
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ActionOnKeySequencePower extends PowerType<ActionOnKeySequencePower.Config> {

    public record Config(
        Optional<EntityAction> successAction,
        Optional<EntityAction> failAction,
        int cooldown,
        HudRender hudRender,
        List<FunctionalKey> keys,
        List<String> keySequence,
        int[] prefixFunction
    ) {}

    private static final class State {
        int progress;
        int cooldown;
        final Set<String> heldLast = new HashSet<>();
    }

    private record StateKey(UUID entity, ResourceLocation powerId) {}

    private final Map<StateKey, State> states = new HashMap<>();

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.optionalFieldOf("success_action").forGetter(Config::successAction),
            EntityAction.CODEC.optionalFieldOf("fail_action").forGetter(Config::failAction),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(Config::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender),
            Codec.list(FunctionalKey.CODEC).fieldOf("keys").forGetter(Config::keys),
            Codec.list(Codec.STRING).fieldOf("key_sequence").forGetter(Config::keySequence)
        ).apply(i, (successAction, failAction, cooldown, hudRender, keys, keySequence) ->
            new Config(successAction, failAction, cooldown, hudRender, keys, keySequence, computePrefix(keySequence))));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        states.remove(new StateKey(holder.rawOwner().getUUID(), powerId));
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!holder.hasPower(powerId)) {
            states.remove(new StateKey(holder.rawOwner().getUUID(), powerId));
        }
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (!(owner instanceof ServerPlayer player)) return;
        if (!(owner.level() instanceof ServerLevel level)) return;

        State state = states.computeIfAbsent(new StateKey(player.getUUID(), powerId), k -> new State());
        EntityCtx ctx = new EntityCtx(player, level);

        if (state.cooldown > 0 || !conditionHolds(powerId, ctx)) {
            if (state.cooldown > 0) state.cooldown--;
            refreshHeld(player.getUUID(), cfg, state);
            return;
        }

        List<String> pressed = null;
        for (FunctionalKey fk : cfg.keys) {
            String keyName = fk.key().key();
            boolean held = HeldKeys.serverHeld(player.getUUID(), keyName);
            boolean wasHeld = state.heldLast.contains(keyName);
            if (held) state.heldLast.add(keyName);
            else state.heldLast.remove(keyName);

            boolean edge = held && !wasHeld;
            boolean actionFires = fk.key().continuous() ? held : edge;
            if (actionFires) fk.action().ifPresent(a -> a.run(ctx));
            if (edge) {
                if (pressed == null) pressed = new ArrayList<>(2);
                pressed.add(keyName);
            }
        }

        if (pressed == null || cfg.keySequence.isEmpty()) return;

        for (String key : pressed) {
            if (feed(key, cfg, state, ctx)) {
                cfg.successAction.ifPresent(a -> a.run(ctx));
                state.progress = 0;
                state.cooldown = Math.max(cfg.cooldown, 0);
                ApoliNetwork.sendActivated(player, new PowerActivatedS2C(powerId, cfg.cooldown));
                break;
            }
        }
    }

    private boolean feed(String key, Config cfg, State state, EntityCtx ctx) {
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

    private void refreshHeld(UUID player, Config cfg, State state) {
        for (FunctionalKey fk : cfg.keys) {
            String keyName = fk.key().key();
            if (HeldKeys.serverHeld(player, keyName)) state.heldLast.add(keyName);
            else state.heldLast.remove(keyName);
        }
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
