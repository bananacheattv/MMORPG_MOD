package com.mmorpg.quest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Donnees des PNJ de quetes (world_npcs.json), sans aucune classe Minecraft : utilisable par les tests unitaires. */
public final class WorldNpcData {
    public static final String RESOURCE = "/data/mmorpg/lore/world_npcs.json";

    public record Entry(String id, String name, int skin, String quest, String dialogue, String dim, String region, int dx, int dz) {
        public UUID uuid() {
            return UUID.nameUUIDFromBytes(("aeloria:world-npc:" + id).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static List<Entry> entries;
    private static Map<String, int[]> anchors;

    private WorldNpcData() {
    }

    public static synchronized List<Entry> entries() {
        if (entries == null) load();
        return entries;
    }

    /** Ancre (x, z) d'une region du monde ouvert. */
    public static int[] anchor(String region) {
        entries();
        return anchors.getOrDefault(region, new int[]{0, 0});
    }

    private static void load() {
        var list = new ArrayList<Entry>();
        var map = new java.util.HashMap<String, int[]>();
        try (Reader r = new InputStreamReader(WorldNpcData.class.getResourceAsStream(RESOURCE), StandardCharsets.UTF_8)) {
            JsonObject root = new Gson().fromJson(r, JsonObject.class);
            root.getAsJsonObject("regions").entrySet().forEach(e -> map.put(e.getKey(), new int[]{e.getValue().getAsJsonArray().get(0).getAsInt(), e.getValue().getAsJsonArray().get(1).getAsInt()}));
            for (var e : root.getAsJsonArray("npcs")) {
                JsonObject o = e.getAsJsonObject();
                list.add(new Entry(o.get("id").getAsString(), o.get("name").getAsString(), o.get("skin").getAsInt(), o.get("quest").getAsString(),
                        o.get("dialogue").getAsString(), o.get("dim").getAsString(), o.get("region").getAsString(), o.get("dx").getAsInt(), o.get("dz").getAsInt()));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("PNJ du monde illisibles : " + ex, ex);
        }
        anchors = map;
        entries = list;
    }

    /** Lie chaque quete a son PNJ (identifiant fixe) : seul ce PNJ la propose. */
    public static void assign(Map<String, QuestDef> quests) {
        for (var e : entries()) {
            var q = quests.get(e.quest());
            if (q != null) q.npc = e.uuid().toString();
        }
    }

}
