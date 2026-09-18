package dev.overgrown.apoli.util;

import net.minecraft.SharedConstants;

import java.util.ArrayList;
import java.util.List;

public final class GameVersion {
    private GameVersion() {}

    private static final class Holder {
        static final String NAME = SharedConstants.getCurrentVersion().getName();
        static final int DATA_VERSION = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    public static String name() {
        return Holder.NAME;
    }

    public static int dataVersion() {
        return Holder.DATA_VERSION;
    }

    public static int compare(String left, String right) {
        List<Object> a = tokenize(left);
        List<Object> b = tokenize(right);
        int n = Math.max(a.size(), b.size());
        for (int i = 0; i < n; i++) {
            Object at = i < a.size() ? a.get(i) : null;
            Object bt = i < b.size() ? b.get(i) : null;
            if (at == null) return bt instanceof String ? 1 : -1;
            if (bt == null) return at instanceof String ? -1 : 1;
            if (at instanceof Long al && bt instanceof Long bl) {
                int cmp = Long.compare(al, bl);
                if (cmp != 0) return cmp;
            } else if (at instanceof String as && bt instanceof String bs) {
                int cmp = as.compareToIgnoreCase(bs);
                if (cmp != 0) return cmp < 0 ? -1 : 1;
            } else {
                return at instanceof String ? -1 : 1;
            }
        }
        return 0;
    }

    private static List<Object> tokenize(String version) {
        int plus = version.indexOf('+');
        String s = (plus < 0 ? version : version.substring(0, plus)).trim();
        List<Object> out = new ArrayList<>(4);
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '.' || c == '-' || c == '_') {
                i++;
            } else if (c >= '0' && c <= '9') {
                long value = 0;
                while (i < n) {
                    char d = s.charAt(i);
                    if (d < '0' || d > '9') break;
                    value = value * 10 + (d - '0');
                    i++;
                }
                out.add(value);
            } else {
                int start = i;
                while (i < n) {
                    char d = s.charAt(i);
                    if (d == '.' || d == '-' || d == '_' || (d >= '0' && d <= '9')) break;
                    i++;
                }
                out.add(s.substring(start, i));
            }
        }
        return out;
    }
}
