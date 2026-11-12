package com.mmorpg.quest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quetes du lore d'Aeloria (plan de quetes v1) : 250 principales, 500 secondaires, 75 evenements saisonniers.
 * Donnees dans {@code data/mmorpg/lore/lore_quests.json}, produites par {@code tools/quetes/build_lore_quests.py}.
 */
public final class LoreQuests {
    public static final String RESOURCE = "/data/mmorpg/lore/lore_quests.json";
    public static final String GROUP_STORY = "histoire", GROUP_CLASS_MASTER = "maitre_classes";
    /** Premiere quete du plan : suit la fin du tutoriel de la Mascotte. */
    public static final String FIRST = "qp_001", AFTER = "tuto_fin";
    private static Map<String, QuestDef> cache;

    private LoreQuests() {
    }

    /** Sans accents, minuscules : base de comparaison des noms de donneurs. */
    public static String norm(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static final String[] DONOR_KEYS = {"ysmeralde", "thalgrim", "brokkar", "orvald", "selian", "halvine", "ilda", "ombrelle", "bondimonde",
            "maitre de classe", "maitre des metiers", "tableau des primes", "annonce serveur", "cadet egare", "inscription ancelienne", "rumeur"};

    /** Toutes les cles de donneurs connues. */
    public static java.util.List<String> allKeys() {
        return java.util.Arrays.stream(DONOR_KEYS).map(k -> k.replace(' ', '_').replace("maitre_de_classe", "maitre_de_classe")).toList();
    }

    /** Cle de donneur d'un nom de PNJ (ou groupe), ou "" : les PNJ dont le nom contient celui du donneur proposent ses quetes. */
    public static String donorKey(String npcName) {
        String n = norm(npcName);
        for (String k : DONOR_KEYS) if (n.contains(k)) return k.replace(' ', '_');
        return "";
    }

    /** Cles de donneur d'un PNJ : par son nom, et par son groupe (« maitre_classes »). */
    public static java.util.Set<String> keysOf(String name, String group) {
        var keys = new java.util.LinkedHashSet<String>();
        String n = norm(name);
        for (String k : DONOR_KEYS) if (n.contains(k)) keys.add(k.replace(' ', '_'));
        if (GROUP_CLASS_MASTER.equals(group)) keys.add("maitre_de_classe");
        return keys;
    }

    public static synchronized Map<String, QuestDef> all() {
        if (cache == null) cache = build();
        // copie profonde : les appelants (ConfigManager) modifient les definitions
        Gson g = new Gson();
        Map<String, QuestDef> copy = new LinkedHashMap<>();
        cache.forEach((id, d) -> copy.put(id, g.fromJson(g.toJson(d), QuestDef.class)));
        return copy;
    }

    private static Map<String, QuestDef> build() {
        Map<String, QuestDef> m = new LinkedHashMap<>();
        try (Reader r = new InputStreamReader(LoreQuests.class.getResourceAsStream(RESOURCE), StandardCharsets.UTF_8)) {
            JsonArray arr = new Gson().fromJson(r, JsonArray.class);
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                m.put(o.get("id").getAsString(), convert(o));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Quetes du lore illisibles : " + ex, ex);
        }
        return m;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    static QuestDef convert(JsonObject o) {
        QuestDef d = new QuestDef();
        QuestDef.Lore l = d.lore = new QuestDef.Lore();
        l.kind = str(o, "kind");
        l.code = str(o, "code");
        l.donor = str(o, "donor");
        l.donorKey = donorKey(l.donor);
        if (l.donorKey.isEmpty()) l.donorKey = norm(l.donor).replaceAll("\\W+", "_");
        l.type = str(o, "type");
        l.objective = str(o, "obj");
        l.region = str(o, "region");
        l.act = str(o, "act");
        l.chapter = str(o, "chapter");
        l.category = str(o, "category");
        l.companion = str(o, "companion");
        l.gameplay = str(o, "gameplay");
        l.event = str(o, "event");
        l.from = str(o, "from");
        l.to = str(o, "to");
        l.conditionText = str(o, "condText");
        d.name = str(o, "name");
        d.minLevel = Math.max(1, Math.min(100, o.get("level").getAsInt()));
        for (JsonElement r : o.getAsJsonArray("requires")) d.requires.add(r.getAsString());
        if (o.has("variants")) o.getAsJsonObject("variants").entrySet().forEach(en -> l.variants.put(en.getKey(), en.getValue().getAsString()));
        if (o.has("classes")) for (JsonElement c : o.getAsJsonArray("classes")) d.classes.add(c.getAsString());
        if (o.has("cond")) {
            JsonObject c = o.getAsJsonObject("cond");
            if (c.has("when")) l.when = c.get("when").getAsString();
            if (c.has("limit")) l.limit = c.get("limit").getAsInt();
            if (c.has("slot")) {
                JsonArray s = c.getAsJsonArray("slot");
                l.slot = new int[]{s.get(0).getAsInt(), s.get(1).getAsInt(), s.get(2).getAsInt(), s.get(3).getAsInt()};
            }
        }
        String repeat = str(o, "repeat");
        d.repeat = repeat;
        if ("daily".equals(repeat)) d.daily = true;
        JsonObject rw = o.getAsJsonObject("rewards");
        d.rewards.xp = rw.get("xp").getAsLong();
        d.rewards.gold = rw.get("gold").getAsLong();
        // « XP adaptee au niveau » : pourcentage de l'XP du niveau en cours (comme les quetes journalieres historiques)
        if (rw.get("xpAdapt").getAsBoolean()) d.rewards.xpPercentOfLevel = 12;
        d.rewards.evolution = rw.get("evolution").getAsInt();
        for (JsonElement x : rw.getAsJsonArray("extras")) l.extras.add(x.getAsString());
        // donneur : trame principale = proposee automatiquement ; maitre de classe = groupe du hub ; le reste = PNJ du meme nom
        boolean main = "principale".equals(l.kind);
        if (main) {
            d.giver = GROUP_STORY;
            d.auto = true;
        }
        // objectifs verifiables, sinon etape narrative : parler au donneur
        for (JsonElement e : o.getAsJsonArray("objs")) {
            JsonArray a = e.getAsJsonArray();
            String type = a.get(0).getAsString(), target = a.get(1).getAsString();
            int count = a.get(2).getAsInt();
            if (type.equals("job")) d.objectives.add(new QuestDef.Objective("action", "job:" + target + "|" + jobLabel(target, count), count));
            else d.objectives.add(new QuestDef.Objective(type, target, count));
        }
        if ("Choix de classe".equals(l.type)) {          // la classe est deja choisie a l'arrivee : etape rendue d'office
            d.giver = GROUP_STORY;
            d.auto = true;
        } else if (d.objectives.isEmpty() && !l.donor.isEmpty()) {
            d.objectives.add(new QuestDef.Objective("action", "lore:" + l.donorKey + "|" + narrativeLabel(l), 1));
        }
        d.description = description(d, str(o, "desc"));
        d.revision = DefaultQuests.REVISION;
        if (main && d.requires.isEmpty()) d.requires.add(AFTER);
        return d;
    }

    private static String jobLabel(String job, int count) {
        return switch (job) {
            case "mineur" -> "Extraire du minerai";
            case "fermier" -> "Récolter des plantes";
            default -> "Pêcher";
        };
    }

    private static String narrativeLabel(QuestDef.Lore l) {
        String t = l.type;
        if (l.donor.startsWith("Maître de classe")) return "Parler à un Maître des classes";
        return switch (t) {
            case "Dialogue", "Compagnon" -> "Parler à " + l.donor;
            default -> "Accomplir l'objectif, puis en rendre compte à " + l.donor;
        };
    }

    private static String description(QuestDef d, String desc) {
        QuestDef.Lore l = d.lore;
        StringBuilder sb = new StringBuilder();
        if (!desc.isEmpty()) sb.append(desc).append("\n\n");
        sb.append("Objectif : ").append(l.objective);
        if (!l.region.isEmpty()) sb.append("\nRégion : ").append(l.region);
        if (!l.conditionText.isEmpty()) sb.append("\nCondition : ").append(l.conditionText);
        if (!l.gameplay.isEmpty()) sb.append("\nNouveauté : ").append(l.gameplay);
        if (!l.companion.isEmpty()) sb.append("\nCompagnon : ").append(l.companion);
        if (!l.extras.isEmpty()) sb.append("\nEn plus : ").append(String.join(", ", l.extras));
        return sb.toString();
    }
}
