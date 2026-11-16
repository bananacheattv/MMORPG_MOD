package com.mmorpg.quest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Quetes de recolte / livraison du lore : chaque objet a rapporter doit etre un vrai objet du mod, et le texte doit dire quoi, combien et a qui. */
class LoreCollectTest {
    @Test
    void everyCollectTargetIsARealItemAndTheTextSaysWhatToBring() throws Exception {
        int n = 0;
        try (var r = new InputStreamReader(LoreCollectTest.class.getResourceAsStream(LoreQuests.RESOURCE), StandardCharsets.UTF_8)) {
            for (var e : new Gson().fromJson(r, JsonArray.class)) {
                var q = e.getAsJsonObject();
                String id = q.get("id").getAsString(), obj = q.get("obj").getAsString();
                for (var oe : q.getAsJsonArray("objs")) {
                    var o = oe.getAsJsonArray();
                    if (!o.get(0).getAsString().equals("collect")) continue;
                    n++;
                    String item = o.get(1).getAsString();
                    int count = o.get(2).getAsInt();
                    assertTrue(LoreCollectTest.class.getResource("/assets/mmorpg/items/" + item + ".json") != null, id + " : objet inexistant " + item);
                    assertTrue(count > 0 && count <= 20, id + " : quantite " + count);
                    assertTrue(obj.contains(String.valueOf(count)) && obj.contains("rapporter"), id + " : texte flou");
                }
            }
        }
        assertTrue(n >= 70, "quetes de recolte converties : " + n);
    }
}
