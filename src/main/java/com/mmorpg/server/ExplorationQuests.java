package com.mmorpg.server;

import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.LevelSystem;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Quetes cachees d'exploration : aucune annonce, aucun objectif affiche. Les lieux secrets (biomes rares, altitudes extremes,
 * autres dimensions) se decouvrent simplement en y allant, a la maniere d'un succes. Fonctionne sur n'importe quelle graine
 * du monde. L'etat vit dans {@code dailyQuests} sous la cle {@code explore:<id>} ; verifie uniquement cote serveur.
 */
@EventBusSubscriber(modid = com.mmorpg.MMORPG.MODID)
public final class ExplorationQuests {
    private static final String KEY = "explore:";
    private static final int[] MILESTONES = {5, 10, 15, 20};

    /** Un lieu secret : {@code biomes} (fragments d'identifiant de biome), altitude et dimension optionnelles. */
    record Spot(String id, String name, String lore, String dimension, String[] biomes, int minY, int maxY) {
    }

    private static Spot biome(String id, String name, String lore, String... biomes) {
        return new Spot(id, name, lore, "minecraft:overworld", biomes, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    static final List<Spot> SPOTS = List.of(
            new Spot("cime_vents", "La cime des vents", "Là où seuls les aigles se reposent.", "minecraft:overworld", new String[0], 180, Integer.MAX_VALUE),
            new Spot("abysses", "Les abysses", "Sous la pierre, un monde sans lumière.", "minecraft:overworld", new String[0], Integer.MIN_VALUE, -40),
            biome("mer_sable", "La mer de sable", "Un océan sans eau, ni fin.", "desert"),
            biome("terres_feu", "Les terres de feu", "Des falaises rouges brûlées par des soleils oubliés.", "badlands"),
            biome("jungle", "La jungle des murmures", "Les feuilles y parlent aux imprudents.", "jungle"),
            biome("cerisiers", "Le bosquet des cerisiers", "Un secret rose, loin des routes.", "cherry_grove"),
            biome("champignons", "L'île aux champignons", "Aucun monstre n'y a jamais posé le pied.", "mushroom_fields"),
            biome("cristal_glace", "La plaine de cristal", "La glace y pousse comme des lames.", "ice_spikes"),
            biome("silence_profond", "Le silence profond", "Ne faites pas de bruit.", "deep_dark"),
            biome("jardin_souterrain", "Le jardin souterrain", "Une forêt cachée sous les montagnes.", "lush_caves"),
            biome("pierre_pendante", "Les grottes de pierre", "Des colonnes de pierre gouttent depuis mille ans.", "dripstone_caves"),
            biome("marais", "Le marais maudit", "Les eaux stagnantes gardent leurs morts.", "swamp", "mangrove_swamp"),
            biome("large", "Au large", "Plus de rivage en vue.", "deep_ocean", "deep_cold_ocean", "deep_lukewarm_ocean", "deep_frozen_ocean"),
            biome("sommets_geles", "Les sommets gelés", "Le toit blanc du royaume.", "frozen_peaks", "jagged_peaks", "stony_peaks"),
            biome("acacias", "Les terres des acacias", "Un soleil de cuivre sur des arbres tordus.", "savanna"),
            biome("foret_loups", "La forêt des loups", "Les aïeux de la meute y dorment encore.", "old_growth_pine_taiga", "old_growth_spruce_taiga"),
            biome("prairies", "Les prairies fleuries", "Un tapis de couleurs au bout du monde.", "flower_forest", "sunflower_plains", "meadow"),
            biome("rivage_oublie", "Le rivage oublié", "Des falaises de pierre grise battues par la mer.", "stony_shore"),
            new Spot("porte_flammes", "La porte des flammes", "Un royaume sous le royaume.", "minecraft:the_nether", new String[0], Integer.MIN_VALUE, Integer.MAX_VALUE),
            new Spot("bord_vide", "Au bord du vide", "Là où le monde s'arrête.", "minecraft:the_end", new String[0], Integer.MIN_VALUE, Integer.MAX_VALUE));

    private ExplorationQuests() {
    }

    public static int total() {
        return SPOTS.size();
    }

    public static int found(PlayerData d) {
        int n = 0;
        for (Spot s : SPOTS) if (d.dailyQuests.containsKey(KEY + s.id)) n++;
        return n;
    }

    private static boolean matches(ServerPlayer p, Spot s) {
        if (!p.level().dimension().identifier().toString().equals(s.dimension)) return false;
        int y = p.getBlockY();
        if (y < s.minY || y > s.maxY) return false;
        if (s.biomes.length == 0) return true;
        String biome = p.level().getBiome(p.blockPosition()).unwrapKey().map(k -> k.identifier().toString()).orElse("");
        for (String b : s.biomes) if (biome.contains(b)) return true;
        return false;
    }

    /** Verifie un lieu pour un joueur ; renvoie vrai s'il vient d'etre decouvert. Utilisable par les tests. */
    static boolean discover(ServerPlayer p, PlayerData d, Spot s) {
        if (d.dailyQuests.containsKey(KEY + s.id)) return false;
        d.dailyQuests.put(KEY + s.id, 1L);
        d.dirty = true;
        long xp = Math.round(LevelSystem.xpToNext(d.level) * 0.20);
        RpgPlayers.addGold(p, 120L + d.level * 6L, true);
        if (xp > 0) RpgPlayers.giveXp(p, xp, false);
        int n = found(d);
        Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.DISCOVERY, "LIEU SECRET DÉCOUVERT", s.name + " (" + n + "/" + SPOTS.size() + ")", 0x80E0B0));
        p.level().playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.CHIME.get(), SoundSource.PLAYERS, 1f, 1.2f);
        for (int i = 0; i < MILESTONES.length; i++) {
            if (n != MILESTONES[i]) continue;
            switch (i) {
                case 0 -> RpgPlayers.give(p, com.mmorpg.crafting.ForgeRecipes.resolveItem("mmorpg:pierre_amelioration"), 2);
                case 1 -> RpgPlayers.give(p, com.mmorpg.crafting.ForgeRecipes.resolveItem("mmorpg:elixir_experience"), 1);
                case 2 -> RpgPlayers.give(p, com.mmorpg.crafting.ForgeRecipes.resolveItem("mmorpg:pierre_amelioration_sup"), 2);
                default -> RpgPlayers.give(p, com.mmorpg.crafting.ForgeRecipes.resolveItem("mmorpg:fragment_divin"), 1);
            }
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("✦ Explorateur : " + n + " lieux secrets découverts, bonus reçu !").withColor(0xFFD040));
        }
        return true;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 40 != 11) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || p.isSpectator() || MountManager.inDungeon(p) || !FeatureFlags.allow(p, "quetes")) continue;
            PlayerData d = RpgPlayers.get(p);
            if (!d.playerClass.isPlayable()) continue;
            for (Spot s : SPOTS) if (!d.dailyQuests.containsKey(KEY + s.id) && matches(p, s)) discover(p, d, s);
        }
    }

    /** Lieux deja decouverts (jamais les autres : ils restent caches), pour l'onglet « Découvertes » du journal. */
    public static ListTag tag(ServerPlayer p, PlayerData d) {
        ListTag list = new ListTag();
        for (Spot s : SPOTS) {
            if (!d.dailyQuests.containsKey(KEY + s.id)) continue;
            CompoundTag t = new CompoundTag();
            t.putString("id", "explore_" + s.id);
            t.putString("name", s.name);
            t.putString("desc", s.lore);
            t.putString("giver", "");
            t.putInt("minLevel", 1);
            t.putBoolean("daily", false);
            t.putBoolean("active", false);
            t.putBoolean("completable", false);
            t.put("objectives", new ListTag());
            CompoundTag rw = new CompoundTag();
            rw.putLong("xp", 0);
            rw.putLong("gold", 0);
            rw.put("items", new ListTag());
            t.put("rewards", rw);
            list.add(t);
        }
        return list;
    }
}
