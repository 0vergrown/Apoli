package dev.overgrown.apoli.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.ErrorLocator;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.dev.DevMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LoadProblems {

    private static final int SHOWN = 8;
    private static final int KEPT = 1000;
    private static final Object LOCK = new Object();
    private static final List<Problem> PROBLEMS = new ArrayList<>();
    private static final Set<String> SEEN = new HashSet<>();
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
    private static WeakReference<ResourceManager> current = new WeakReference<>(null);
    private static volatile boolean pending;

    private record Context(String kind, ResourceLocation id, @Nullable JsonDataFiles.Source source, List<String> warnings) {}

    private static final java.util.regex.Pattern MISSING_KEY = java.util.regex.Pattern.compile("No key ([A-Za-z0-9_]+)");
    private static final java.util.regex.Pattern FIELD_CHAIN =
        java.util.regex.Pattern.compile("The '([^']+)' field(?: of \\S+)? is present but failed to parse(?:, so it was ignored and everything else loaded)?: ");

    public record Problem(boolean error, String kind, String id, @Nullable String file, @Nullable String pack,
                          List<String> reasons, @Nullable String path, JsonTextParser.Location location) {

        public String headline() {
            String first = reasons.isEmpty() ? "unknown error" : reasons.get(0);
            if (first.length() > 200) first = first.substring(0, 197) + "...";
            return kind + " " + id + ": " + first;
        }

        public @Nullable String fileAndLine() {
            if (file == null) return null;
            return location == null ? file : file + ":" + location.line();
        }

        public String details() {
            StringBuilder out = new StringBuilder();
            out.append(error ? "Couldn't load " : "Problem in ").append(kind).append(' ').append(id);
            if (file != null) {
                out.append(" from ").append(file);
                if (pack != null) out.append(" (pack ").append(pack).append(')');
            }
            if (path != null || location != null) {
                out.append("\n  at ");
                if (path != null) out.append(path);
                if (location != null) {
                    if (path != null) out.append(", ");
                    out.append("line ").append(location.line()).append(", column ").append(location.column());
                    String number = Integer.toString(location.line());
                    out.append("\n    ").append(number).append(" | ").append(location.text());
                    out.append("\n    ").append(" ".repeat(number.length())).append(" | ")
                        .append(" ".repeat(Math.max(0, location.caret()))).append('^');
                }
            }
            for (String reason : reasons) out.append("\n  - ").append(reason);
            return out.toString();
        }
    }

    private LoadProblems() {}

    public static void begin(ResourceManager manager) {
        synchronized (LOCK) {
            if (current.get() == manager) return;
            current = new WeakReference<>(manager);
            PROBLEMS.clear();
            SEEN.clear();
            pending = true;
        }
    }

    public static String kindOf(String directory) {
        String kind = directory;
        int slash = kind.lastIndexOf('/');
        if (slash >= 0) kind = kind.substring(slash + 1);
        if (kind.endsWith("s") && !kind.endsWith("ss")) kind = kind.substring(0, kind.length() - 1);
        return kind.replace('_', ' ');
    }

    public static String kindOf(JsonDataFiles.Source source) {
        String path = source.file().getPath();
        int slash = path.indexOf('/');
        return kindOf(slash < 0 ? path : path.substring(0, slash));
    }

    public static <A, T> Optional<A> parse(ResourceLocation id, JsonDataFiles.Source source, Decoder<A> decoder, Dynamic<T> input) {
        return parse(kindOf(source), id, source, "$", decoder, input);
    }

    public static <A, T> Optional<A> parse(String kind, ResourceLocation id, @Nullable JsonDataFiles.Source source,
                                           String rootPath, Decoder<A> decoder, Dynamic<T> input) {
        Context previous = CONTEXT.get();
        String previousField = LoggedOptionalField.context();
        Context context = new Context(kind, id, source, new ArrayList<>(0));
        CONTEXT.set(context);
        LoggedOptionalField.setContext(id);
        try {
            DataResult<A> result = decoder.parse(input);
            for (String warning : context.warnings()) {
                record(false, kind, id, source, warning, locate(decoder, input, warning, rootPath));
            }
            Optional<A> value = result.result();
            if (value.isPresent()) return value;
            String message = result.error().map(error -> error.message()).orElse("unknown error");
            record(true, kind, id, source, message, locate(decoder, input, message, rootPath));
            return result.resultOrPartial(error -> {});
        } finally {
            if (previous == null) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(previous);
            }
            if (previousField == null) {
                LoggedOptionalField.clearContext();
            } else {
                LoggedOptionalField.setContext(ResourceLocation.tryParse(previousField));
            }
        }
    }

    private static <T> @Nullable String locate(Decoder<?> decoder, Dynamic<T> input, String message, String rootPath) {
        if (!(input.getValue() instanceof JsonElement json)) return null;
        String path = ErrorLocator.locate(decoder, json, message);
        if (path != null && !"$".equals(rootPath)) path = rootPath + path.substring(1);
        return path;
    }

    public static void error(String kind, ResourceLocation id, @Nullable JsonDataFiles.Source source, String message) {
        record(true, kind, id, source, message, null);
    }

    public static void warn(String kind, ResourceLocation id, @Nullable JsonDataFiles.Source source, String message) {
        record(false, kind, id, source, message, null);
    }

    public static void error(String kind, ResourceLocation id, @Nullable JsonDataFiles.Source source, String message,
                             @Nullable String path) {
        record(true, kind, id, source, message, path);
    }

    public static void warn(String kind, ResourceLocation id, @Nullable JsonDataFiles.Source source, String message,
                            @Nullable String path) {
        record(false, kind, id, source, message, path);
    }

    public static boolean warnInContext(String message) {
        Context context = CONTEXT.get();
        if (context == null) return false;
        if (context.warnings().size() < 32) context.warnings().add(message);
        return true;
    }

    static void syntax(JsonDataFiles.Source source, ResourceLocation id, String message, @Nullable String path,
                       JsonTextParser.Location location) {
        add(new Problem(true, kindOf(source), id.toString(), fileOf(source), source.packId(),
            List.of(message), path, location));
    }

    private static void record(boolean error, String kind, ResourceLocation id, @Nullable JsonDataFiles.Source source,
                               String message, @Nullable String path) {
        JsonTextParser.Location location = source != null && path != null ? JsonDataFiles.locate(source, path) : null;
        add(new Problem(error, kind, id.toString(), source == null ? null : fileOf(source),
            source == null ? null : source.packId(), reasons(message), path, location));
    }

    private static void add(Problem problem) {
        String key = problem.kind() + '|' + problem.id() + '|' + problem.file() + '|' + problem.pack() + '|' + problem.reasons();
        synchronized (LOCK) {
            if (!SEEN.add(key)) return;
            if (PROBLEMS.size() < KEPT) PROBLEMS.add(problem);
        }
        if (problem.error()) {
            Apoli.LOGGER.error("[Apoli] {}", problem.details());
        } else {
            Apoli.LOGGER.warn("[Apoli] {}", problem.details());
        }
    }

    private static List<String> reasons(String message) {
        String tidy = ErrorLocator.tidy(message);
        java.util.regex.Matcher chain = FIELD_CHAIN.matcher(tidy);
        String field = null;
        int end = 0;
        while (chain.find()) {
            if (chain.start() != end) break;
            field = chain.group(1);
            end = chain.end();
        }
        boolean dropped = tidy.startsWith("The '") && tidy.contains("so it was ignored and everything else loaded");
        if (field != null) {
            tidy = (dropped ? "Ignored the '" + field + "' field, everything else loaded. " : "'" + field + "': ") + tidy.substring(end);
        }
        List<String> out = new ArrayList<>(2);
        for (String part : tidy.split("; ")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("Second: ") && !out.isEmpty()) {
                out.set(out.size() - 1, out.get(out.size() - 1) + " / " + trimmed.substring(8));
                continue;
            }
            trimmed = MISSING_KEY.matcher(trimmed.replace("Failed to parse either. First: ", "")).replaceAll("Missing the required '$1' field");
            if (!out.contains(trimmed)) out.add(trimmed);
        }
        if (out.isEmpty()) out.add(tidy);
        return List.copyOf(out);
    }

    private static String fileOf(JsonDataFiles.Source source) {
        ResourceLocation file = source.file();
        return "data/" + file.getNamespace() + "/" + file.getPath();
    }

    public static List<Problem> snapshot() {
        synchronized (LOCK) {
            return List.copyOf(PROBLEMS);
        }
    }

    public static int count(String kind, boolean errors) {
        int count = 0;
        synchronized (LOCK) {
            for (Problem problem : PROBLEMS) {
                if (problem.error() == errors && problem.kind().equals(kind)) count++;
            }
        }
        return count;
    }

    public static void tick(MinecraftServer server) {
        if (!pending) return;
        pending = false;
        List<Problem> problems = snapshot();
        if (!problems.isEmpty()) {
            int errors = 0;
            Map<String, Integer> byKind = new LinkedHashMap<>();
            for (Problem problem : problems) {
                if (problem.error()) errors++;
                byKind.merge(problem.kind(), 1, Integer::sum);
            }
            int warnings = problems.size() - errors;
            StringBuilder kinds = new StringBuilder();
            for (Map.Entry<String, Integer> entry : byKind.entrySet()) {
                if (!kinds.isEmpty()) kinds.append(", ");
                kinds.append(entry.getKey()).append(' ').append(entry.getValue());
            }
            Apoli.LOGGER.warn("[Apoli] Data packs loaded with {} error{} and {} warning{} ({}). Each one is logged above with its file and line; /apoli:dev_mode shows them in chat.",
                errors, errors == 1 ? "" : "s", warnings, warnings == 1 ? "" : "s", kinds);
        }
        DevMode.announce(server, lines(true));
    }

    public static List<Component> lines(boolean afterReload) {
        List<Problem> problems = snapshot();
        if (problems.isEmpty()) {
            if (!afterReload) return List.of();
            return List.of(Component.literal("[apoli] data packs loaded with no errors or warnings")
                .withStyle(ChatFormatting.DARK_AQUA));
        }
        int errors = 0;
        for (Problem problem : problems) if (problem.error()) errors++;
        int warnings = problems.size() - errors;
        List<Component> out = new ArrayList<>(2 + Math.min(SHOWN, problems.size()));
        out.add(Component.literal("[apoli] data pack load: " + errors + (errors == 1 ? " error, " : " errors, ")
                + warnings + (warnings == 1 ? " warning" : " warnings") + " (hover a line for details, click to copy the file)")
            .withStyle(errors > 0 ? ChatFormatting.RED : ChatFormatting.GOLD));
        int shown = 0;
        for (int pass = 0; pass < 2 && shown < SHOWN; pass++) {
            for (Problem problem : problems) {
                if (shown >= SHOWN) break;
                if (problem.error() != (pass == 0)) continue;
                out.add(line(problem));
                shown++;
            }
        }
        int hidden = problems.size() - shown;
        if (hidden > 0) {
            out.add(Component.literal("[apoli] ...and " + hidden + " more in the server log").withStyle(ChatFormatting.GRAY));
        }
        return out;
    }

    private static Component line(Problem problem) {
        String where = problem.fileAndLine();
        MutableComponent text = Component.literal("[apoli] " + problem.headline()
            + (where == null ? "" : " (" + shortName(where) + ")"));
        String details = problem.details();
        return text.withStyle(style -> {
            style = style.withColor(problem.error() ? ChatFormatting.RED : ChatFormatting.GOLD)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(details)));
            return where == null ? style : style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, where));
        });
    }

    private static String shortName(String where) {
        int slash = where.lastIndexOf('/');
        return slash < 0 ? where : where.substring(slash + 1);
    }
}
