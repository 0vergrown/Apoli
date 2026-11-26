package dev.overgrown.apoli.compat.spellengine.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.internals.container.SpellContainerSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SpellsPower extends PowerType<SpellsPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("spells");

    private static final String KEY_PREFIX = "apoli_power:";
    // Spells from powers with "always": their active spells can be cast whatever the player holds (SpellContainerSourceMixin).
    public static final String ALWAYS_PREFIX = "apoli_power_always:";
    private static final int CHECK_INTERVAL = 10;

    public record Config(List<ResourceLocation> spells, boolean always) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            SingleOrList.of(IdCodecs.ID).fieldOf("spells").forGetter(Config::spells),
            Codec.BOOL.optionalFieldOf("always", false).forGetter(Config::always)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        reconcile(powerId, cfg, holder);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        apply(powerId, cfg, holder.owner(), false);
    }

    @Override
    public void onSuppressed(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        apply(powerId, cfg, holder.owner(), false);
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (owner == null || (owner.tickCount + powerId.hashCode()) % CHECK_INTERVAL != 0) return;
        reconcile(powerId, cfg, holder);
    }

    private static void reconcile(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (!(owner instanceof ServerPlayer)) return;
        boolean active = holder.hasPower(powerId) && !holder.isSuppressed(powerId);
        if (active) {
            Power power = ApoliPowers.get(powerId);
            active = power != null && (power.condition().isEmpty()
                || power.condition().get().test(EntityCtx.of(owner, owner.level())));
        }
        apply(powerId, cfg, owner, active);
    }

    private static void apply(ResourceLocation powerId, Config cfg, LivingEntity owner, boolean grant) {
        if (!(owner instanceof ServerPlayer player)) return;
        SpellContainerSource.Owner spells = (SpellContainerSource.Owner) player;
        Map<String, SpellContainer> containers = spells.serverSideSpellContainers();
        String key = (cfg.always() ? ALWAYS_PREFIX : KEY_PREFIX) + powerId;
        String otherKey = (cfg.always() ? KEY_PREFIX : ALWAYS_PREFIX) + powerId;
        boolean changed = containers.remove(otherKey) != null;
        SpellContainer existing = containers.get(key);
        if (grant) {
            if (existing == null || !sameSpells(existing.spell_ids(), cfg.spells())) {
                List<String> ids = new ArrayList<>(cfg.spells().size());
                for (ResourceLocation id : cfg.spells()) ids.add(id.toString());
                containers.put(key, cfg.always()
                    ? new SpellContainer(SpellContainer.ContentType.ANY, "", "", ids.size(), ids)
                    : new SpellContainer(SpellContainer.ContentType.NONE, "", "", 0, ids));
                changed = true;
            }
        } else if (existing != null) {
            containers.remove(key);
            changed = true;
        }
        if (changed) spells.markServerSideSpellContainersDirty();
    }

    private static boolean sameSpells(List<String> granted, List<ResourceLocation> wanted) {
        if (granted.size() != wanted.size()) return false;
        for (int i = 0, n = granted.size(); i < n; i++) {
            if (!granted.get(i).equals(wanted.get(i).toString())) return false;
        }
        return true;
    }

    public static void sweep(ServerPlayer player) {
        SpellContainerSource.Owner spells = (SpellContainerSource.Owner) player;
        PowerContainer container = PowerContainer.of(player);
        boolean changed = spells.serverSideSpellContainers().keySet().removeIf(key -> {
            boolean always = key.startsWith(ALWAYS_PREFIX);
            if (!always && !key.startsWith(KEY_PREFIX)) return false;
            ResourceLocation powerId = ResourceLocation.tryParse(key.substring((always ? ALWAYS_PREFIX : KEY_PREFIX).length()));
            if (powerId == null || container == null || !container.hasPower(powerId)) return true;
            Power power = ApoliPowers.get(powerId);
            return power == null || !(power.config() instanceof Config cfg) || cfg.always() != always;
        });
        if (changed) spells.markServerSideSpellContainersDirty();
    }
}
