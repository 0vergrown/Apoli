package dev.overgrown.apoli.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.overgrown.apoli.keybind.Keybind;
import dev.overgrown.apoli.mixin.keybinding.KeyMappingCategoryAccessor;
import dev.overgrown.apoli.mixin.keybinding.OptionsAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class DynamicKeyMappingManager {
    private static final Logger LOG = LogUtils.getLogger();

    private static final List<KeyMapping> SESSION_BINDINGS = new ArrayList<>();
    private static final Map<String, String> NAME_HINTS = new HashMap<>();

    private DynamicKeyMappingManager() {}

    public static synchronized void applyKeybinds(List<Keybind> definitions) {
        unregisterAll();

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            LOG.warn("[Apoli] Options not yet initialized; skipping dynamic keybind registration.");
            return;
        }

        for (Keybind def : definitions) {
            String translationKey = def.translationKey();
            if (isAlreadyRegistered(translationKey, mc.options.keyMappings)) {
                LOG.debug("[Apoli] '{}' already present in Options.keyMappings — skipping.", translationKey);
                continue;
            }

            InputConstants.Key defaultKey = parseKey(def.key(), def.id().toString());
            ensureCategoryExists(def.category());

            KeyMapping km = new KeyMapping(translationKey, defaultKey.getType(), defaultKey.getValue(), def.category());
            restoreSavedBinding(km, new File(mc.gameDirectory, "options.txt"));

            KeyMapping[] current = mc.options.keyMappings;
            KeyMapping[] extended = Arrays.copyOf(current, current.length + 1);
            extended[current.length] = km;
            ((OptionsAccessor) mc.options).apoli$setKeyMappings(extended);

            SESSION_BINDINGS.add(km);
            def.name().ifPresent(n -> NAME_HINTS.put(translationKey, n));

            LOG.debug("[Apoli] Registered keybind '{}' (default {}, category {}).",
                translationKey, def.key(), def.category());
        }

        KeyMapping.resetMapping();
        LOG.info("[Apoli] {} dynamic keybind(s) active for this session.", SESSION_BINDINGS.size());
    }

    public static synchronized void unregisterAll() {
        if (SESSION_BINDINGS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            Options options = mc.options;
            if (options != null) {
                Set<KeyMapping> remove = Collections.newSetFromMap(new IdentityHashMap<>());
                remove.addAll(SESSION_BINDINGS);
                KeyMapping[] filtered = Arrays.stream(options.keyMappings)
                    .filter(km -> !remove.contains(km))
                    .toArray(KeyMapping[]::new);
                ((OptionsAccessor) options).apoli$setKeyMappings(filtered);
                KeyMapping.resetMapping();
            }
        }
        SESSION_BINDINGS.clear();
        NAME_HINTS.clear();
    }

    public static String nameHint(String translationKey) {
        return NAME_HINTS.get(translationKey);
    }

    private static boolean isAlreadyRegistered(String translationKey, KeyMapping[] all) {
        for (KeyMapping km : all) {
            if (translationKey.equals(km.getName())) return true;
        }
        return false;
    }

    private static InputConstants.Key parseKey(String name, String owner) {
        try {
            return InputConstants.getKey(name);
        } catch (Exception e) {
            LOG.warn("[Apoli] Could not parse key '{}' for '{}': {}", name, owner, e.getMessage());
            return InputConstants.UNKNOWN;
        }
    }

    private static void ensureCategoryExists(String category) {
        Map<String, Integer> map = KeyMappingCategoryAccessor.apoli$getCategorySortOrder();
        if (!map.containsKey(category)) {
            int next = map.values().stream().max(Integer::compareTo).orElse(0) + 1;
            map.put(category, next);
            LOG.debug("[Apoli] Registered new controls category '{}'.", category);
        }
    }

    private static void restoreSavedBinding(KeyMapping km, File optionsFile) {
        if (optionsFile == null || !optionsFile.exists()) return;
        String prefix = "key_" + km.getName() + ":";
        try (BufferedReader reader = new BufferedReader(new FileReader(optionsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(prefix)) continue;
                String savedKey = line.substring(prefix.length()).trim();
                try {
                    km.setKey(InputConstants.getKey(savedKey));
                } catch (Exception ex) {
                    LOG.warn("[Apoli] Ignoring invalid saved binding '{}' for '{}': {}",
                        savedKey, km.getName(), ex.getMessage());
                }
                return;
            }
        } catch (IOException ex) {
            LOG.warn("[Apoli] Could not read options.txt to restore binding for '{}': {}",
                km.getName(), ex.getMessage());
        }
    }
}
