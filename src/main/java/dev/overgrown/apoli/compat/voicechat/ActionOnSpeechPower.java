package dev.overgrown.apoli.compat.voicechat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.power.PowerType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ActionOnSpeechPower extends PowerType<ActionOnSpeechPower.Config> {
    private static final long COOLDOWN_MS = 250L;
    private static final Map<UUID, Map<String, Long>> RECENT = new HashMap<>();

    public record Config(
        Optional<EntityAction> entityAction,
        Optional<String> message,
        List<String> messages,
        Optional<Pattern> regex,
        Optional<String> language
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            Codec.STRING.optionalFieldOf("message").forGetter(Config::message),
            Codec.STRING.listOf().optionalFieldOf("messages", List.of()).forGetter(Config::messages),
            Codec.STRING.xmap(s -> Pattern.compile(dev.overgrown.apoli.data.message.TranslationKeyResolver.expandPattern(s)), Pattern::pattern).optionalFieldOf("regex").forGetter(Config::regex),
            Codec.STRING.optionalFieldOf("language").forGetter(Config::language)
        ).apply(i, Config::new));
    }

    public static void fire(net.minecraft.server.level.ServerPlayer player, String text, String language) {
        long now = System.currentTimeMillis();
        Map<String, Long> recent = RECENT.computeIfAbsent(player.getUUID(), u -> new HashMap<>());
        dev.overgrown.apoli.power.PowerLookup.forEach(player, dev.overgrown.apoli.power.ApoliIds.ACTION_ON_SPEECH,
            Config.class, cfg -> {
                String trigger = matchedTrigger(cfg, text, language);
                if (trigger == null) {
                    return;
                }
                String key = System.identityHashCode(cfg) + trigger;
                Long last = recent.get(key);
                if (last != null && now - last < COOLDOWN_MS) {
                    return;
                }
                recent.put(key, now);
                cfg.entityAction().ifPresent(action ->
                    action.run(new dev.overgrown.apoli.condition.context.EntityCtx(player, player.serverLevel())));
            });
        recent.entrySet().removeIf(entry -> now - entry.getValue() > COOLDOWN_MS * 4L);
        if (recent.isEmpty()) {
            RECENT.remove(player.getUUID());
        }
    }

    public static void fireTrigger(net.minecraft.server.level.ServerPlayer player, net.minecraft.resources.ResourceLocation powerId) {
        dev.overgrown.apoli.power.PowerContainer container = dev.overgrown.apoli.power.PowerContainer.of(player);
        if (container == null || !container.hasPower(powerId) || container.isSuppressed(powerId)) return;
        dev.overgrown.apoli.power.Power power = dev.overgrown.apoli.power.ApoliPowers.get(powerId);
        if (power == null || !(power.config() instanceof Config cfg)) return;
        if (!dev.overgrown.apoli.power.ApoliIds.ACTION_ON_SPEECH.equals(
            dev.overgrown.apoli.power.PowerTypeRegistry.resolveId(power.typeId()))) return;

        dev.overgrown.apoli.condition.context.EntityCtx ctx =
            new dev.overgrown.apoli.condition.context.EntityCtx(player, player.serverLevel());
        if (power.condition().isPresent() && !power.condition().get().test(ctx)) return;

        long now = System.currentTimeMillis();
        Map<String, Long> recent = RECENT.computeIfAbsent(player.getUUID(), u -> new HashMap<>());
        String key = powerId.toString();
        Long last = recent.get(key);
        if (last != null && now - last < COOLDOWN_MS) return;
        recent.put(key, now);
        prune(recent, now, player.getUUID());

        cfg.entityAction().ifPresent(action -> action.run(ctx));
    }

    private static void prune(Map<String, Long> recent, long now, UUID uuid) {
        recent.entrySet().removeIf(entry -> now - entry.getValue() > COOLDOWN_MS * 4L);
        if (recent.isEmpty()) RECENT.remove(uuid);
    }

    public static void forget(UUID uuid) {
        RECENT.remove(uuid);
    }

    public static boolean matches(Config config, String text, String language) {
        return matchedTrigger(config, text, language) != null;
    }

    private static boolean languageMatches(String want, String got) {
        if (got == null || got.isEmpty()) {
            return false;
        }
        String w = want.toLowerCase(Locale.ROOT);
        String g = got.toLowerCase(Locale.ROOT);
        return g.equals(w) || g.startsWith(w + "-") || w.startsWith(g + "-");
    }

    public static String matchedTrigger(Config config, String text, String language) {
        if (config.language().isPresent() && !languageMatches(config.language().get(), language)) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        boolean any = true;
        if (config.message().isPresent()) {
            any = false;
            String phrase = config.message().get().toLowerCase(Locale.ROOT);
            if (lower.contains(phrase)) {
                return "m:" + phrase;
            }
        }
        for (String message : config.messages()) {
            any = false;
            String phrase = message.toLowerCase(Locale.ROOT);
            if (lower.contains(phrase)) {
                return "m:" + phrase;
            }
        }
        if (config.regex().isPresent()) {
            any = false;
            if (config.regex().get().matcher(text).find()) {
                return "r:" + config.regex().get().pattern();
            }
        }
        return any ? "*" : null;
    }
}
