package dev.overgrown.apoli.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class JsonTextParser {
    private static final int EOF = -1;
    private static final JsonPrimitive TRUE = new JsonPrimitive(true);
    private static final JsonPrimitive FALSE = new JsonPrimitive(false);
    private static final JsonPrimitive NAN = new JsonPrimitive(Double.NaN);
    private static final JsonPrimitive POSITIVE_INFINITY = new JsonPrimitive(Double.POSITIVE_INFINITY);
    private static final JsonPrimitive NEGATIVE_INFINITY = new JsonPrimitive(Double.NEGATIVE_INFINITY);

    private final char[] text;
    private final int end;
    private final JsonFormat format;
    private int pos;
    private JsonElement[] containers = new JsonElement[16];
    private String[] names = new String[16];
    private int depth;

    public record Parsed(JsonElement value, int trailingLine) {}

    private JsonTextParser(char[] text, int start, int end, JsonFormat format) {
        this.text = text;
        this.pos = start;
        this.end = end;
        this.format = format;
    }

    public static Parsed parse(String text, JsonFormat format) {
        char[] chars = text.toCharArray();
        return new JsonTextParser(chars, 0, chars.length, format).document();
    }

    public static Parsed read(InputStream in, JsonFormat format) throws IOException {
        return read(in.readAllBytes(), format);
    }

    public static Parsed read(byte[] utf8, JsonFormat format) {
        CharBuffer chars = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(utf8));
        return new JsonTextParser(chars.array(), chars.arrayOffset() + chars.position(),
            chars.arrayOffset() + chars.limit(), format).document();
    }

    private Parsed document() {
        if (pos < end && text[pos] == '﻿') pos++;
        int first = next();
        if (first == EOF) throw error("The file has no value in it");
        JsonElement value = value(first);
        int trailing = trailingContent();
        return new Parsed(value, trailing < 0 ? 0 : lineOf(trailing));
    }

    private JsonElement value(int first) {
        int c = first;
        while (true) {
            JsonElement done;
            if (c == '{') {
                int n = next();
                if (n == '}') {
                    done = new JsonObject();
                } else {
                    push(new JsonObject());
                    names[depth - 1] = name(n);
                    colon();
                    c = next();
                    continue;
                }
            } else if (c == '[') {
                int n = next();
                if (n == ']') {
                    done = new JsonArray();
                } else {
                    push(new JsonArray());
                    c = n;
                    continue;
                }
            } else {
                done = scalar(c);
            }
            while (true) {
                if (depth == 0) return done;
                JsonElement parent = containers[depth - 1];
                int n;
                if (parent instanceof JsonArray array) {
                    array.add(done);
                    n = next();
                    if (n == ',') {
                        n = next();
                        if (n == ']') {
                            trailingComma();
                            done = pop();
                            continue;
                        }
                        c = n;
                        break;
                    }
                    if (n == ']') {
                        done = pop();
                        continue;
                    }
                    throw unexpected(n, "Expected ',' or ']' after an array element");
                }
                ((JsonObject) parent).add(names[depth - 1], done);
                n = next();
                if (n == ',') {
                    n = next();
                    if (n == '}') {
                        trailingComma();
                        done = pop();
                        continue;
                    }
                    names[depth - 1] = name(n);
                    colon();
                    c = next();
                    break;
                }
                if (n == '}') {
                    done = pop();
                    continue;
                }
                throw unexpected(n, "Expected ',' or '}' after a value");
            }
        }
    }

    private String name(int c) {
        if (c == '"') return string('"');
        if (c == '\'') {
            requireJson5("single-quoted names");
            return string('\'');
        }
        if (c == EOF) throw error("The file ends inside an object");
        if (!startsUnquotedName((char) c)) {
            pos--;
            throw error("Expected a name, found " + describe(c));
        }
        if (format != JsonFormat.JSON5) {
            pos--;
            throw error("Use a .json5 file for names without quotes");
        }
        int start = pos - 1;
        int i = pos;
        while (i < end && !endsUnquotedName(i)) i++;
        pos = i;
        return new String(text, start, i - start);
    }

    private void colon() {
        int c = next();
        if (c != ':') throw unexpected(c, "Expected ':' after the name");
    }

    private JsonElement scalar(int c) {
        switch (c) {
            case '"':
                return new JsonPrimitive(string('"'));
            case '\'':
                requireJson5("single-quoted strings");
                return new JsonPrimitive(string('\''));
            case EOF:
                throw error("Expected a value, but the file ends here");
            case ',':
            case ':':
            case ']':
            case '}':
                pos--;
                throw error("Expected a value, found " + describe(c));
            default:
                pos--;
                return literal();
        }
    }

    private JsonElement literal() {
        char c = text[pos];
        if ((c == 't' || c == 'T') && keyword("true")) return TRUE;
        if ((c == 'f' || c == 'F') && keyword("false")) return FALSE;
        if ((c == 'n' || c == 'N') && keyword("null")) return JsonNull.INSTANCE;
        return number();
    }

    private boolean keyword(String word) {
        int length = word.length();
        if (pos + length > end) return false;
        for (int i = 1; i < length; i++) {
            char c = text[pos + i];
            char lower = word.charAt(i);
            if (c != lower && c != lower - 32) return false;
        }
        int after = pos + length;
        if (after < end && !terminates(after)) {
            if (Character.isLetterOrDigit(text[after])) return false;
            terminated(after);
        }
        pos = after;
        return true;
    }

    private JsonElement number() {
        int start = pos;
        int i = pos;
        char c = text[i];
        boolean negative = false;
        if (c == '+' || c == '-') {
            if (c == '+') requireJson5("a leading '+'");
            negative = c == '-';
            i++;
            if (i >= end) {
                pos = i;
                throw error("Expected a number after the sign");
            }
            c = text[i];
        }
        if (c == 'I' || c == 'N') {
            String word = c == 'I' ? "Infinity" : "NaN";
            if (!text(i, word)) {
                pos = i;
                throw unquoted(c);
            }
            requireJson5(word);
            terminated(i + word.length());
            pos = i + word.length();
            if (c == 'N') return NAN;
            return negative ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
        }
        if (c == '0' && i + 1 < end && (text[i + 1] == 'x' || text[i + 1] == 'X')) {
            requireJson5("hexadecimal numbers");
            int digits = i + 2;
            int j = digits;
            while (j < end && Character.digit(text[j], 16) >= 0) j++;
            if (j == digits) {
                pos = j;
                throw error("Expected hexadecimal digits after '0x'");
            }
            terminated(j);
            pos = j;
            String hex = new String(text, digits, j - digits);
            String decimal = j - digits <= 15
                ? Long.toString(negative ? -Long.parseLong(hex, 16) : Long.parseLong(hex, 16))
                : (negative ? new BigInteger(hex, 16).negate() : new BigInteger(hex, 16)).toString();
            return new JsonPrimitive(new LazyNumber(decimal, true));
        }
        int integerStart = i;
        while (i < end && isDigit(text[i])) i++;
        int integerDigits = i - integerStart;
        if (integerDigits == 0 && (i >= end || text[i] != '.')) {
            pos = i;
            if (i > start) throw error("Expected a number after the sign");
            throw unquoted(c);
        }
        if (integerDigits > 1 && text[integerStart] == '0') {
            pos = integerStart;
            throw error("Numbers can't have leading zeros");
        }
        int fractionDigits = -1;
        boolean exponent = false;
        if (i < end && text[i] == '.') {
            i++;
            int fractionStart = i;
            while (i < end && isDigit(text[i])) i++;
            fractionDigits = i - fractionStart;
            if (integerDigits == 0 && fractionDigits == 0) {
                pos = i;
                throw error("Expected digits around the decimal point");
            }
            if (integerDigits == 0) requireJson5("a number starting with '.'");
            if (fractionDigits == 0) requireJson5("a number ending with '.'");
        }
        if (i < end && (text[i] == 'e' || text[i] == 'E')) {
            exponent = true;
            i++;
            if (i < end && (text[i] == '+' || text[i] == '-')) i++;
            int exponentStart = i;
            while (i < end && isDigit(text[i])) i++;
            if (i == exponentStart) {
                pos = i;
                throw error("Expected digits in the exponent");
            }
        }
        terminated(i);
        pos = i;
        boolean integral = fractionDigits <= 0 && !exponent;
        return new JsonPrimitive(new LazyNumber(numberText(start, i, integerStart, integerDigits, fractionDigits), integral));
    }

    private String numberText(int start, int stop, int integerStart, int integerDigits, int fractionDigits) {
        int from = text[start] == '+' ? start + 1 : start;
        if (integerDigits > 0 && fractionDigits != 0) return new String(text, from, stop - from);
        StringBuilder out = new StringBuilder(stop - from + 1);
        out.append(text, from, integerStart - from);
        if (integerDigits == 0) out.append('0');
        int point = integerStart + integerDigits;
        out.append(text, integerStart, integerDigits);
        if (fractionDigits == 0) {
            out.append(text, point + 1, stop - point - 1);
        } else {
            out.append(text, point, stop - point);
        }
        return out.toString();
    }

    private boolean text(int at, String word) {
        if (at + word.length() > end) return false;
        for (int i = 0; i < word.length(); i++) {
            if (text[at + i] != word.charAt(i)) return false;
        }
        return true;
    }

    private void terminated(int at) {
        if (at >= end || terminates(at)) return;
        pos = at;
        char c = text[at];
        if (c == '/' && at + 1 < end && (text[at + 1] == '/' || text[at + 1] == '*')) {
            throw error("Use a .jsonc or .json5 file for comments");
        }
        throw error("Unexpected " + describe(c) + " after the value");
    }

    private boolean terminates(int at) {
        char c = text[at];
        switch (c) {
            case '{', '}', '[', ']', ':', ',', ' ', '\t', '\n', '\r', '\f':
                return true;
            case '/':
                return format.allowsComments() && at + 1 < end && (text[at + 1] == '/' || text[at + 1] == '*');
            case '\u000B':
                return format == JsonFormat.JSON5;
            default:
                return format == JsonFormat.JSON5 && c > 0x7F && isExtendedSpace(c);
        }
    }

    private String string(char quote) {
        int start = pos;
        for (int i = start; i < end; i++) {
            char c = text[i];
            if (c == quote) {
                pos = i + 1;
                return new String(text, start, i - start);
            }
            if (c == '\\') return escaped(quote, start, i);
        }
        pos = start - 1;
        throw error("A string is never closed");
    }

    private String escaped(char quote, int start, int from) {
        StringBuilder out = new StringBuilder(from - start + 16);
        out.append(text, start, from - start);
        int i = from;
        while (i < end) {
            char c = text[i++];
            if (c == quote) {
                pos = i;
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (i >= end) break;
            char escape = text[i++];
            switch (escape) {
                case 'u' -> {
                    if (i + 4 > end) {
                        pos = i - 2;
                        throw error("A \\u escape needs four hexadecimal digits");
                    }
                    int value = 0;
                    for (int k = 0; k < 4; k++) {
                        int digit = Character.digit(text[i + k], 16);
                        if (digit < 0 || text[i + k] > 0x7F) {
                            pos = i - 2;
                            throw error("A \\u escape needs four hexadecimal digits");
                        }
                        value = (value << 4) | digit;
                    }
                    out.append((char) value);
                    i += 4;
                }
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 'f' -> out.append('\f');
                case '\n', '\r', '\'', '"', '\\', '/' -> out.append(escape);
                default -> {
                    pos = i - 2;
                    throw error("Unknown escape sequence \\" + escape);
                }
            }
        }
        pos = start - 1;
        throw error("A string is never closed");
    }

    private int next() {
        while (pos < end) {
            char c = text[pos++];
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
            if (c == '/') {
                if (pos >= end) return c;
                char after = text[pos];
                if (after != '/' && after != '*') return c;
                if (!format.allowsComments()) {
                    pos--;
                    throw error("Use a .jsonc or .json5 file for comments");
                }
                skipComment(after);
                continue;
            }
            if (c == '\f' || c == '\u000B' || (c > 0x7F && isExtendedSpace(c))) {
                if (format != JsonFormat.JSON5) {
                    pos--;
                    throw error("Use a .json5 file for U+" + hex(c) + " as whitespace");
                }
                continue;
            }
            return c;
        }
        return EOF;
    }

    private void skipComment(char kind) {
        int open = pos - 1;
        pos++;
        if (kind == '/') {
            while (pos < end) {
                char c = text[pos++];
                if (c == '\n' || c == '\r') return;
            }
            return;
        }
        while (pos + 1 < end) {
            if (text[pos] == '*' && text[pos + 1] == '/') {
                pos += 2;
                return;
            }
            pos++;
        }
        pos = open;
        throw error("A /* comment is never closed");
    }

    private int trailingContent() {
        int i = pos;
        while (i < end) {
            char c = text[i];
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t'
                || (format == JsonFormat.JSON5 && (c == '\f' || c == '\u000B' || (c > 0x7F && isExtendedSpace(c))))) {
                i++;
                continue;
            }
            if (c == '/' && i + 1 < end && format.allowsComments()) {
                if (text[i + 1] == '/') {
                    i += 2;
                    while (i < end && text[i] != '\n' && text[i] != '\r') i++;
                    continue;
                }
                if (text[i + 1] == '*') {
                    int close = indexOfCommentEnd(i + 2);
                    if (close < 0) return i;
                    i = close + 2;
                    continue;
                }
            }
            return i;
        }
        return -1;
    }

    private int indexOfCommentEnd(int from) {
        for (int i = from; i + 1 < end; i++) {
            if (text[i] == '*' && text[i + 1] == '/') return i;
        }
        return -1;
    }

    private void trailingComma() {
        if (!format.allowsComments()) {
            pos--;
            throw error("Use a .jsonc or .json5 file for trailing commas");
        }
    }

    private void requireJson5(String what) {
        if (format != JsonFormat.JSON5) throw error("Use a .json5 file for " + what);
    }

    private void push(JsonElement container) {
        if (depth == containers.length) {
            containers = Arrays.copyOf(containers, depth * 2);
            names = Arrays.copyOf(names, depth * 2);
        }
        containers[depth] = container;
        names[depth] = null;
        depth++;
    }

    private JsonElement pop() {
        depth--;
        JsonElement container = containers[depth];
        containers[depth] = null;
        names[depth] = null;
        return container;
    }

    private boolean startsUnquotedName(char c) {
        switch (c) {
            case '{', '}', '[', ']', ':', ',', '/', '\\', ';', '#', '=', '"', '\'':
                return false;
            default:
                return c > ' ';
        }
    }

    private boolean endsUnquotedName(int at) {
        char c = text[at];
        switch (c) {
            case '{', '}', '[', ']', ':', ',', ' ', '\t', '\n', '\r', '\f', '\u000B':
                return true;
            case '/':
                return at + 1 < end && (text[at + 1] == '/' || text[at + 1] == '*');
            default:
                return c > 0x7F && isExtendedSpace(c);
        }
    }

    private static boolean isExtendedSpace(char c) {
        return c == ' ' || c == ' ' || c == ' ' || c == '﻿'
            || Character.getType(c) == Character.SPACE_SEPARATOR;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private JsonParseException unquoted(char c) {
        if (Character.isLetter(c)) return error("Text values need quotes around them");
        return error("Unexpected " + describe(c));
    }

    private JsonParseException unexpected(int c, String message) {
        if (c == EOF) return error("The file ends before every object and array is closed");
        pos--;
        return error(message + ", found " + describe(c));
    }

    private static String describe(int c) {
        if (c == EOF) return "the end of the file";
        if (c < ' ') return "U+" + hex((char) c);
        return "'" + (char) c + "'";
    }

    private static String hex(char c) {
        String digits = Integer.toHexString(c).toUpperCase(Locale.ROOT);
        return "0000".substring(digits.length()) + digits;
    }

    private int lineOf(int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < end; i++) {
            if (text[i] == '\n') line++;
        }
        return line;
    }

    private JsonParseException error(String message) {
        int at = Math.max(0, Math.min(pos, end));
        int line = 1;
        int lineStart = 0;
        for (int i = 0; i < at; i++) {
            if (text[i] == '\n') {
                line++;
                lineStart = i + 1;
            }
        }
        return new JsonParseException(message + " at line " + line + " column " + (at - lineStart + 1) + " path " + path());
    }

    private String path() {
        StringBuilder out = new StringBuilder("$");
        for (int d = 0; d < depth; d++) {
            if (containers[d] instanceof JsonArray array) {
                out.append('[').append(array.size()).append(']');
            } else {
                out.append('.');
                if (names[d] != null) out.append(names[d]);
            }
        }
        return out.toString();
    }

    @SuppressWarnings("serial")
    private static final class LazyNumber extends Number {
        private final String value;
        private final boolean integral;

        private LazyNumber(String value, boolean integral) {
            this.value = value;
            this.integral = integral;
        }

        @Override
        public int intValue() {
            if (integral) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    try {
                        return (int) Long.parseLong(value);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return new BigDecimal(value).intValue();
        }

        @Override
        public long longValue() {
            if (integral) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException ignored) {
                }
            }
            return new BigDecimal(value).longValue();
        }

        @Override
        public float floatValue() {
            return Float.parseFloat(value);
        }

        @Override
        public double doubleValue() {
            return Double.parseDouble(value);
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof LazyNumber number && value.equals(number.value);
        }
    }
}
