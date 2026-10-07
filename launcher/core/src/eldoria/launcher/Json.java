package eldoria.launcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mini parseur / serialiseur JSON (Map, List, String, Double, Boolean, null) sans dependance. */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        Json p = new Json(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) throw p.err("fin attendue");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object o) { return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>(); }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o) { return o instanceof List ? (List<Object>) o : new ArrayList<>(); }

    public static String str(Object o, String def) { return o == null ? def : String.valueOf(o); }

    private RuntimeException err(String m) { return new IllegalArgumentException("JSON invalide (" + m + ") a la position " + i); }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private Object value() {
        if (i >= s.length()) throw err("valeur attendue");
        char c = s.charAt(i);
        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '"') return string();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (start == i) throw err("caractere inattendu '" + c + "'");
        return Double.parseDouble(s.substring(start, i));
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        ws();
        if (s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            String k = string();
            ws();
            if (s.charAt(i++) != ':') throw err("':' attendu");
            ws();
            m.put(k, value());
            ws();
            char c = s.charAt(i++);
            if (c == '}') return m;
            if (c != ',') throw err("',' ou '}' attendu");
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++;
        ws();
        if (s.charAt(i) == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ']') return l;
            if (c != ',') throw err("',' ou ']' attendu");
        }
    }

    private String string() {
        if (s.charAt(i) != '"') throw err("chaine attendue");
        i++;
        StringBuilder b = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c != '\\') { b.append(c); continue; }
            char e = s.charAt(i++);
            switch (e) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'r' -> b.append('\r');
                case 'b' -> b.append('\b');
                case 'f' -> b.append('\f');
                case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                default -> b.append(e);
            }
        }
    }

    public static String write(Object o) {
        StringBuilder b = new StringBuilder();
        write(b, o, 0);
        return b.toString();
    }

    private static void write(StringBuilder b, Object o, int ind) {
        if (o == null) b.append("null");
        else if (o instanceof String str) quote(b, str);
        else if (o instanceof Double d && d == Math.rint(d) && !Double.isInfinite(d)) b.append(d.longValue());
        else if (o instanceof Number || o instanceof Boolean) b.append(o);
        else if (o instanceof Map<?, ?> m) {
            if (m.isEmpty()) { b.append("{}"); return; }
            b.append("{\n");
            int n = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                b.append("  ".repeat(ind + 1));
                quote(b, String.valueOf(e.getKey()));
                b.append(" : ");
                write(b, e.getValue(), ind + 1);
                b.append(++n < m.size() ? ",\n" : "\n");
            }
            b.append("  ".repeat(ind)).append('}');
        } else if (o instanceof List<?> l) {
            if (l.isEmpty()) { b.append("[]"); return; }
            b.append("[\n");
            for (int k = 0; k < l.size(); k++) {
                b.append("  ".repeat(ind + 1));
                write(b, l.get(k), ind + 1);
                b.append(k + 1 < l.size() ? ",\n" : "\n");
            }
            b.append("  ".repeat(ind)).append(']');
        } else quote(b, o.toString());
    }

    private static void quote(StringBuilder b, String str) {
        b.append('"');
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        b.append('"');
    }
}
