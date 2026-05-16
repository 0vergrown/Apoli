package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ActionOnKeyPressPower extends PowerType<ActionOnKeyPressPower.Config> {
    private final Map<CooldownKey, Integer> cooldowns = new HashMap<>();

    public record Config(EntityAction entityAction, int cooldown, HudRender hudRender, Key key) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Config::entityAction),
            Codec.INT.optionalFieldOf("cooldown", 1).forGetter(Config::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender),
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Config::key)
        ).apply(i, Config::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        CooldownKey key = new CooldownKey(holder.owner().getUUID(), powerId);
        Integer remaining = cooldowns.get(key);
        if (remaining == null) return;
        if (remaining <= 1) cooldowns.remove(key);
        else cooldowns.put(key, remaining - 1);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!holder.allPowers().contains(powerId)) {
            cooldowns.remove(new CooldownKey(holder.owner().getUUID(), powerId));
        }
    }

    public boolean tryActivate(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (!(owner.level() instanceof ServerLevel level)) return false;
        CooldownKey key = new CooldownKey(owner.getUUID(), powerId);
        if (cooldowns.getOrDefault(key, 0) > 0) return false;
        cfg.entityAction.run(new EntityCtx(owner, level));
        if (cfg.cooldown > 0) cooldowns.put(key, cfg.cooldown);
        return true;
    }

    public int getCooldown(LivingEntity owner, ResourceLocation powerId) {
        return cooldowns.getOrDefault(new CooldownKey(owner.getUUID(), powerId), 0);
    }

    private record CooldownKey(UUID entity, ResourceLocation powerId) {}
}
