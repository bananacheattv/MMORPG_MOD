package com.mmorpg.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Amenagement du grand Spawn (config/mmorpg/spawn.json) : ou se trouvent les marchands, les maitres (mines, donjons) et les stations
 * (banque, hotel des ventes, presse, forges, teleporteur). Les positions sont en blocs depuis le centre de la place du marche (x : est,
 * z : sud). Deplacez, ajoutez ou retirez des entrees, ou coupez {@code autoNpcs} / {@code autoStations} pour tout poser a la main :
 * rien n'est alors place ni retire a la construction du spawn. {@code /spawn pnj} et {@code /spawn stations} appliquent le fichier sans
 * reconstruire le terrain.
 */
public class SpawnConfig {
    /** Manual construction: never generate or upgrade the automatic market. */
    public boolean manualBuild = false;
    public boolean flatPlatform = false;
    /** Pose (et remplace) les PNJ de la liste a la construction du spawn ; faux : aucun PNJ n'est pose, aucun n'est retire. */
    public boolean autoNpcs = true;
    /** Pose les stations de la liste a la construction du spawn ; faux : aucune n'est posee (a placer a la main, onglet Objets). */
    public boolean autoStations = true;
    public List<Npc> npcs = new ArrayList<>();
    public List<Station> stations = new ArrayList<>();

    /** Un PNJ : role (MARCHAND, MINES, DONJON, QUETES), nom affiche, apparence (0 a 3), boutique ou groupe, position et regard (0 sud, 90 ouest, 180 nord, 270 est). */
    public static class Npc {
        public String id = "";
        public String role = "MARCHAND";
        public String name = "Marchand";
        public int skin = 0;
        public String group = "";
        public double x;
        public double z;
        public float yaw;

        public Npc() {
        }

        public Npc(String id, String role, String name, int skin, String group, double x, double z, float yaw) {
            this.id = id;
            this.role = role;
            this.name = name;
            this.skin = skin;
            this.group = group;
            this.x = x;
            this.z = z;
            this.yaw = yaw;
        }
    }

    /** Une station : bloc 3D du mod (banque_aeloria, hdv_aeloria, presse_compression, forge_arcanique, teleporteur), position et face (north, south, east, west). */
    public static class Station {
        public String id = "";
        public String block = "forge_arcanique";
        public int x;
        public int z;
        public String facing = "north";

        public Station() {
        }

        public Station(String id, String block, int x, int z, String facing) {
            this.id = id;
            this.block = block;
            this.x = x;
            this.z = z;
            this.facing = facing;
        }
    }

    public static SpawnConfig defaults() {
        SpawnConfig c = new SpawnConfig();
        c.npcs.add(new Npc("alchimie", "MARCHAND", "Alchimiste", 3, "marche:alchimie", 12, -13, 0F));
        c.npcs.add(new Npc("graines", "MARCHAND", "Marchand de graines", 1, "marche:graines", 12, 13, 180F));
        c.npcs.add(new Npc("armurerie", "MARCHAND", "Armurier", 0, "marche:armurerie", -12, -13, 0F));
        c.npcs.add(new Npc("minerais", "MARCHAND", "Marchand de minerais", 2, "marche:minerais", -12, 13, 180F));
        c.npcs.add(new Npc("mines", "MINES", "Maître des mines", 1, "marche:mines", 23, -18, 90F));
        c.npcs.add(new Npc("donjons", "DONJON", "Maître des donjons", 3, "donjon:*", 23, 19, 90F));
        c.npcs.add(new Npc("ferme", "MARCHAND", "Fermière d'Aeloria", 2, "marche:ferme", -29, 23, 180F));
        c.stations.add(new Station("banque", "banque_aeloria", 0, -25, "south"));
        c.stations.add(new Station("hotel_des_ventes", "hdv_aeloria", -27, 0, "east"));
        c.stations.add(new Station("presse", "presse_compression", 0, 24, "north"));
        c.stations.add(new Station("forge_ouest", "forge_arcanique", -21, 25, "north"));
        c.stations.add(new Station("forge_est", "forge_arcanique", 22, 25, "north"));
        c.stations.add(new Station("teleporteur", "teleporteur", 25, 0, "west"));
        return c;
    }

    /** Corrige un fichier incomplet ou modifie a la main (listes absentes, valeurs vides). */
    public SpawnConfig sanitize() {
        if (npcs == null) npcs = new ArrayList<>();
        if (stations == null) stations = new ArrayList<>();
        npcs.removeIf(n -> n == null || n.id == null || n.id.isBlank());
        stations.removeIf(s -> s == null || s.id == null || s.id.isBlank() || s.block == null);
        for (Npc n : npcs) {
            if (n.role == null || n.role.isBlank()) n.role = "MARCHAND";
            if (n.name == null) n.name = "";
            if (n.group == null) n.group = "";
            n.skin = Math.max(0, Math.min(3, n.skin));
        }
        for (Station s : stations) if (s.facing == null || s.facing.isBlank()) s.facing = "north";
        return this;
    }
}
