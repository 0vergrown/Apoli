package dev.overgrown.apoli.data.sound;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.Apoli;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SoundReplacements {

    private static final Pattern REGEX_METACHARACTERS = Pattern.compile("[\\\\^$|?*+()\\[\\]{}]");
    private static final Pattern PLAIN_PATH = Pattern.compile("[a-z0-9_.\\-/]+");

    private record Choice(List<WeightedSound> options, int[] cumulative, boolean captureReferences) {
        WeightedSound pick(RandomSource random) {
            if (options.size() == 1) return options.get(0);
            int total = cumulative[cumulative.length - 1];
            int roll = random.nextInt(total);
            for (int i = 0; i < cumulative.length; i++) {
                if (roll < cumulative[i]) return options.get(i);
            }
            return options.get(options.size() - 1);
        }
    }

    private record Rule(Pattern pattern, Choice choice) {}

    private final Map<String, List<WeightedSound>> raw;
    private final Map<String, Choice> exact;
    private final List<Rule> rules;

    private SoundReplacements(Map<String, List<WeightedSound>> raw, Map<String, Choice> exact, List<Rule> rules) {
        this.raw = raw;
        this.exact = exact;
        this.rules = rules;
    }

    public boolean isEmpty() {
        return exact.isEmpty() && rules.isEmpty();
    }

    public @Nullable WeightedSound find(ResourceLocation soundId, RandomSource random) {
        Choice direct = exact.get(soundId.toString());
        if (direct != null) return direct.pick(random);
        if (rules.isEmpty()) return null;

        String name = soundId.toString();
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            if (!rule.choice.captureReferences) {
                if (rule.pattern.matcher(name).matches()) return rule.choice.pick(random);
                continue;
            }
            Matcher matcher = rule.pattern.matcher(name);
            if (!matcher.matches()) continue;
            WeightedSound picked = rule.choice.pick(random);
            try {
                return picked.withId(matcher.replaceFirst(picked.id()));
            } catch (RuntimeException e) {
                Apoli.LOGGER.warn("[Apoli] Sound replacement \"{}\" for pattern \"{}\" has a bad capture group reference: {}",
                    picked.id(), rule.pattern.pattern(), e.getMessage());
                return picked;
            }
        }
        return null;
    }

    private static SoundReplacements compile(Map<String, List<WeightedSound>> raw) {
        Map<String, Choice> exact = new LinkedHashMap<>(raw.size());
        List<Rule> rules = new ArrayList<>();
        for (Map.Entry<String, List<WeightedSound>> entry : raw.entrySet()) {
            String key = entry.getKey();
            List<WeightedSound> options = entry.getValue();
            if (options.isEmpty()) continue;
            Choice choice = choice(options);

            if (!REGEX_METACHARACTERS.matcher(key).find()) {
                exact.put(namespaced(key), choice);
                continue;
            }
            try {
                rules.add(new Rule(Pattern.compile(key), choice));
            } catch (PatternSyntaxException e) {
                Apoli.LOGGER.warn("[Apoli] Sound replacement key \"{}\" is not valid regex ({}); matching it literally.",
                    key, e.getDescription());
                exact.put(namespaced(key), choice);
            }
        }
        return new SoundReplacements(raw, exact, List.copyOf(rules));
    }

    private static String namespaced(String key) {
        if (key.indexOf(':') >= 0) return key;
        return PLAIN_PATH.matcher(key).matches() ? "minecraft:" + key : key;
    }

    private static Choice choice(List<WeightedSound> options) {
        int[] cumulative = new int[options.size()];
        int running = 0;
        boolean captures = false;
        for (int i = 0; i < options.size(); i++) {
            WeightedSound sound = options.get(i);
            running += sound.weight();
            cumulative[i] = running;
            if (sound.hasCaptureReference()) captures = true;
        }
        return new Choice(List.copyOf(options), cumulative, captures);
    }

    public static final Codec<SoundReplacements> CODEC = Codec
        .unboundedMap(Codec.STRING, WeightedSound.LIST_CODEC)
        .xmap(SoundReplacements::compile, r -> r.raw);
}
