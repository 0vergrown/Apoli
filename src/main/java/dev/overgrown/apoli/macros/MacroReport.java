package dev.overgrown.apoli.macros;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public record MacroReport(int defined, int calls, int powers, List<String> errors, List<String> warnings) {

    private static final int SHOWN = 8;
    private static volatile MacroReport latest = new MacroReport(0, 0, 0, List.of(), List.of());

    public static MacroReport latest() {
        return latest;
    }

    static void publish(MacroReport report) {
        latest = report;
    }

    public boolean isEmpty() {
        return defined == 0 && calls == 0 && errors.isEmpty() && warnings.isEmpty();
    }

    public List<Component> lines() {
        if (isEmpty()) return List.of();
        List<Component> out = new ArrayList<>(2 + Math.min(SHOWN, errors.size() + warnings.size()));
        StringBuilder summary = new StringBuilder("[apoli] macros: ")
            .append(defined).append(" defined, ")
            .append(calls).append(calls == 1 ? " call" : " calls").append(" expanded in ")
            .append(powers).append(powers == 1 ? " power" : " powers");
        if (!errors.isEmpty()) summary.append(", ").append(errors.size()).append(errors.size() == 1 ? " error" : " errors");
        if (!warnings.isEmpty()) {
            summary.append(", ").append(warnings.size()).append(warnings.size() == 1 ? " warning" : " warnings");
        }
        out.add(Component.literal(summary.toString())
            .withStyle(errors.isEmpty() ? ChatFormatting.DARK_AQUA : ChatFormatting.RED));
        int shown = 0;
        for (int i = 0; i < errors.size() && shown < SHOWN; i++, shown++) {
            out.add(Component.literal("[apoli] " + errors.get(i)).withStyle(ChatFormatting.RED));
        }
        for (int i = 0; i < warnings.size() && shown < SHOWN; i++, shown++) {
            out.add(Component.literal("[apoli] " + warnings.get(i)).withStyle(ChatFormatting.GOLD));
        }
        int hidden = errors.size() + warnings.size() - shown;
        if (hidden > 0) {
            out.add(Component.literal("[apoli] …and " + hidden + " more in the server log").withStyle(ChatFormatting.GRAY));
        }
        return out;
    }
}
