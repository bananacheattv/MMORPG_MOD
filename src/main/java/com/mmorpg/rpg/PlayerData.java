package com.mmorpg.rpg;

import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.pet.PetType;
import com.mmorpg.skill.Skill;
import com.mmorpg.skill.Skills;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Donnees RPG persistantes d'un joueur (attachees au joueur, conservees a la mort).
 * Les champs "transients" (statistiques calculees, recharges, effets) ne sont pas sauvegardes.
 */
public class PlayerData implements ValueIOSerializable {
    public float shieldGuard=100;
    public boolean shieldGuardRecovering;
    public String pvpTitle="";
    public CompoundTag backpacks=new CompoundTag();
    public CompoundTag adventureSummary = new CompoundTag();
    public String rankLabel="Aelorien";public int rankColor=0xB9AFCC,rankTier,auctionSlots=12;
    public CompoundTag guildReceipts = new CompoundTag();
    public int guildLevel;
    public long[] guildBuffs=new long[4];
    public final Set<String> forgeRecipes = new HashSet<>();
    public static final int SKILL_SLOTS = 6;
    private static final Codec<Map<String, Integer>> STR_INT_MAP = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private static final Codec<Map<String, String>> STR_STR_MAP = Codec.unboundedMap(Codec.STRING, Codec.STRING);

    public PlayerClass playerClass = PlayerClass.NONE;
    public int level = 1;
    public long xp = 0;
    public int attributePoints = 0;
    public final int[] allocated = new int[Stat.PRIMARIES.length];
    public final Map<String, Integer> skillRanks = new LinkedHashMap<>();
    public final String[] skillBar = new String[SKILL_SLOTS];
    public float hp = -1;
    public float mana = -1;
    public final Map<String, Integer> petXp = new LinkedHashMap<>();
    public String activePet = "";
    public final Map<String, Integer> petHappiness = new LinkedHashMap<>();

    public int happiness(String id) {
        return Math.clamp(petHappiness.getOrDefault(id, 100), 0, 100);
    }

    public double petBonusMultiplier(String id) {
        return 0.5 + happiness(id) / 200.0;
    }
    public final Set<String> cosmetics = new HashSet<>();
    public final Map<Cosmetic.Category, String> equippedCosmetics = new EnumMap<>(Cosmetic.Category.class);
    public final Set<UUID> waypoints = new HashSet<>();
    public final Map<String, Integer> kills = new HashMap<>();
    public boolean classChosenOnce = false;
    /** Pioche et houe du debutant deja remises (une seule fois ; /outils en redonne si elles sont perdues). */
    public boolean starterTools = false;
    /** Permanent forge scroll licenses (bits 0..4); preserved by save/load, death and network travel. */
    public int forgeBlueprints = 0;
    /** Nombre de mines et de plaines ouvertes au joueur : la premiere l'est d'office, les suivantes se debloquent en trouvant le portail cache de la precedente. */
    public int minesUnlocked = 1;
    public int plainsUnlocked = 1;
    /** Palier d'evolution debloque aupres du Maitre des classes (quetes) ; -1 = ancien joueur pas encore migre. */
    public int evolution = 0;

    /** Palier d'evolution effectif : celui permis par le niveau, plafonne par les quetes d'evolution terminees. */
    public int evolutionTier() {
        return Math.min(PlayerClass.tier(level), Math.max(0, evolution));
    }

    public String title() {
        return playerClass.evolutions[evolutionTier()];
    }
    /** Porte-monnaie (pieces d'or). */
    public long gold = 0;
    public int xpCharmSeconds;
    public int luckCharmSeconds;
    /** Quetes en cours : progression de chaque objectif. */
    public String trackedQuest = "";
    public boolean questGuidanceMuted;
    public final Map<String,CompoundTag> questGivers=new LinkedHashMap<>();
    public final Map<String, int[]> activeQuests = new LinkedHashMap<>();
    public final Set<String> completedQuests = new HashSet<>();
    /** Quetes journalieres : jour (numero de jour du monde) de la derniere realisation. */
    public final Map<String, Long> dailyQuests = new HashMap<>();

    public final Set<String> mounts = new HashSet<>();
    /** Experience cumulee des metiers (pecheur, fermier, mineur). */
    public final Map<String, Long> jobXp = new LinkedHashMap<>();

    // --- transient (non sauvegarde)
    public StatBlock stats = new StatBlock();
    public final Map<String, Long> cooldowns = new HashMap<>();
    public final Map<BuffType, Buff> buffs = new EnumMap<>(BuffType.class);
    public UUID mountEntity;
    public String activeMount = "";
    /** Derniere monture utilisee (touche Monture). */
    public String lastMount = "";
    /** Combat PvP : montures interdites jusqu'a ce tick serveur (non sauvegarde). */
    public long pvpUntilTick;
    public long nextMountTick;
    public UUID petEntity;
    public boolean dirty = true;
    /** Version du profil dans la base de donnees du reseau a la derniere synchronisation (0 : jamais synchronise ; voir db/PlayerSync). */
    public long dbVersion;
    public float regenAccumulatorHp;
    public float regenAccumulatorMana;

    public PlayerData() {
        Arrays.fill(skillBar, "");
    }

    // ------------------------------------------------------------------ competences

    public int skillRank(String id) {
        return skillRanks.getOrDefault(id, 0);
    }

    public boolean isSkillUnlocked(Skill skill) {
        return skill.playerClass == playerClass && level >= skill.unlockLevel && skillRank(skill.id) > 0;
    }

    public int spentSkillPoints() {
        int spent = 0;
        for (int r : skillRanks.values()) {
            spent += Math.max(0, r - 1);
        }
        return spent;
    }

    public int availableSkillPoints() {
        return Math.max(0, LevelSystem.skillPointsAt(level, playerClass) - spentSkillPoints());
    }

    /** Debloque au rang 1 les competences accessibles et les place dans la barre si une case est libre. */
    public List<Skill> unlockSkills() {
        List<Skill> unlocked = new ArrayList<>();
        for (Skill s : Skills.forClass(playerClass)) {
            if (level >= s.unlockLevel && !skillRanks.containsKey(s.id)) {
                skillRanks.put(s.id, 1);
                unlocked.add(s);
                if (!s.passive) {
                    for (int i = 0; i < SKILL_SLOTS; i++) {
                        if (skillBar[i].isEmpty()) {
                            skillBar[i] = s.id;
                            break;
                        }
                    }
                }
            }
        }
        return unlocked;
    }

    public int petLevel(String id) {
        return PetType.levelForXp(petXp.getOrDefault(id, 0));
    }

    public void resetClass() {
        playerClass = PlayerClass.NONE;
        skillRanks.clear();
        Arrays.fill(skillBar, "");
        Arrays.fill(allocated, 0);
        attributePoints = LevelSystem.attributePointsAt(level);
        buffs.clear();
        cooldowns.clear();
        dirty = true;
    }

    public void resetAttributes() {
        Arrays.fill(allocated, 0);
        attributePoints = LevelSystem.attributePointsAt(level);
        dirty = true;
    }

    // ------------------------------------------------------------------ serialisation

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.put("adventure_summary", adventureSummary.copy());
        t.putString("rankLabel",rankLabel);t.putInt("rankColor",rankColor);t.putInt("rankTier",rankTier);t.putInt("auctionSlots",auctionSlots);
        t.putFloat("shieldGuard",shieldGuard);t.putBoolean("shieldGuardRecovering",shieldGuardRecovering);t.putString("pvpTitle",pvpTitle);t.put("backpacks",backpacks.copy());t.put("guild_receipts",guildReceipts.copy());t.putInt("guild_level",guildLevel);
        t.putLongArray("guild_buffs",guildBuffs);
        t.store("forgeRecipes",Codec.STRING.listOf(),new ArrayList<>(forgeRecipes));
        t.putString("class", playerClass.name());
        t.putInt("level", level);
        t.putLong("xp", xp);
        t.putInt("attrPoints", attributePoints);
        t.putIntArray("allocated", allocated);
        t.store("skills", STR_INT_MAP, skillRanks);
        t.store("bar", Codec.STRING.listOf(), Arrays.asList(skillBar));
        t.putFloat("hp", hp);
        t.putFloat("mana", mana);
        t.store("pets", STR_INT_MAP, petXp);
        t.putString("activePet", activePet);
        t.store("mounts", Codec.STRING.listOf(), new ArrayList<>(mounts));
        t.putString("lastMount", lastMount);
        t.store("petHappiness", STR_INT_MAP, petHappiness);
        t.store("cosmetics", Codec.STRING.listOf(), new ArrayList<>(cosmetics));
        Map<String, String> eq = new HashMap<>();
        equippedCosmetics.forEach((k, v) -> eq.put(k.name(), v));
        t.store("equipped", STR_STR_MAP, eq);
        t.store("waypoints", UUIDUtil.CODEC.listOf(), new ArrayList<>(waypoints));
        t.store("kills", STR_INT_MAP, kills);
        t.store("jobXp", Codec.unboundedMap(Codec.STRING, Codec.LONG), jobXp);
        t.putBoolean("chosen", classChosenOnce);
        t.putBoolean("starterTools", starterTools);
        t.putInt("forgeBlueprints", forgeBlueprints);
        t.putInt("minesUnlocked", minesUnlocked);
        t.putInt("plainsUnlocked", plainsUnlocked);
        t.putInt("evolution", evolution);
        t.putLong("gold", gold);
        t.putLong("dbVersion", dbVersion);
        t.putInt("xpCharmSeconds", xpCharmSeconds);
        t.putInt("luckCharmSeconds", luckCharmSeconds);
        Map<String, List<Integer>> aq = new LinkedHashMap<>();
        activeQuests.forEach((k, v) -> {
            List<Integer> l = new ArrayList<>();
            for (int x : v) l.add(x);
            aq.put(k, l);
        });
        t.putString("trackedQuest", trackedQuest);
        t.putBoolean("questGuidanceMuted",questGuidanceMuted);
        t.store("questGivers",Codec.unboundedMap(Codec.STRING,CompoundTag.CODEC),questGivers);
        t.store("activeQuests", Codec.unboundedMap(Codec.STRING, Codec.INT.listOf()), aq);
        t.store("completedQuests", Codec.STRING.listOf(), new ArrayList<>(completedQuests));
        t.store("dailyQuests", Codec.unboundedMap(Codec.STRING, Codec.LONG), dailyQuests);
        return t;
    }

    public void load(CompoundTag t) {
        adventureSummary = t.getCompoundOrEmpty("adventure_summary").copy();
        rankLabel=t.getStringOr("rankLabel","Aelorien");rankColor=t.getIntOr("rankColor",0xB9AFCC);rankTier=t.getIntOr("rankTier",0);auctionSlots=t.getIntOr("auctionSlots",12);
        shieldGuard=Math.clamp(t.getFloatOr("shieldGuard",100),0,100);shieldGuardRecovering=t.getBooleanOr("shieldGuardRecovering",shieldGuard<=0);pvpTitle=t.getStringOr("pvpTitle","");backpacks=t.getCompoundOrEmpty("backpacks").copy();guildReceipts=t.getCompoundOrEmpty("guild_receipts").copy();guildLevel=Math.clamp(t.getIntOr("guild_level",0),0,10);
        guildBuffs=t.getLongArray("guild_buffs").orElse(new long[4]);if(guildBuffs.length!=4)guildBuffs=new long[4];
        forgeRecipes.clear();forgeRecipes.addAll(t.read("forgeRecipes",Codec.STRING.listOf()).orElse(List.of()));
        playerClass = PlayerClass.byName(t.getStringOr("class", "NONE"));
        level = Math.max(1, Math.min(LevelSystem.MAX_LEVEL, t.getIntOr("level", 1)));
        xp = t.getLongOr("xp", 0);
        attributePoints = t.getIntOr("attrPoints", 0);
        int[] a = t.getIntArray("allocated").orElse(new int[0]);          // anciennes sauvegardes : 5 attributs
        System.arraycopy(a, 0, allocated, 0, Math.min(allocated.length, a.length));
        skillRanks.clear();
        skillRanks.putAll(t.read("skills", STR_INT_MAP).orElse(Map.of()));
        Arrays.fill(skillBar, "");
        List<String> bar = t.read("bar", Codec.STRING.listOf()).orElse(List.of());
        for (int i = 0; i < Math.min(SKILL_SLOTS, bar.size()); i++) {
            skillBar[i] = bar.get(i);
        }
        hp = t.getFloatOr("hp", -1);
        mana = t.getFloatOr("mana", -1);
        petXp.clear();
        petXp.putAll(t.read("pets", STR_INT_MAP).orElse(Map.of()));
        activePet = t.getStringOr("activePet", "");
        petHappiness.clear();
        t.read("petHappiness", STR_INT_MAP).orElse(Map.of()).forEach((id, n) -> {
            if (petXp.containsKey(id)) petHappiness.put(id, Math.clamp(n, 0, 100));
        });
        cosmetics.clear();
        cosmetics.addAll(t.read("cosmetics", Codec.STRING.listOf()).orElse(List.of()));
        equippedCosmetics.clear();
        t.read("equipped", STR_STR_MAP).orElse(Map.of()).forEach((k, v) -> {
            try {
                equippedCosmetics.put(Cosmetic.Category.valueOf(k), v);
            } catch (IllegalArgumentException ignored) {
            }
        });
        waypoints.clear();
        waypoints.addAll(t.read("waypoints", UUIDUtil.CODEC.listOf()).orElse(List.of()));
        kills.clear();
        kills.putAll(t.read("kills", STR_INT_MAP).orElse(Map.of()));
        jobXp.clear();
        jobXp.putAll(t.read("jobXp", Codec.unboundedMap(Codec.STRING, Codec.LONG)).orElse(Map.of()));
        classChosenOnce = t.getBooleanOr("chosen", playerClass != PlayerClass.NONE);
        starterTools = t.getBooleanOr("starterTools", false);
        forgeBlueprints = t.getIntOr("forgeBlueprints", 0) & 31;
        if (forgeBlueprints != 0) {
            for (var recipe : com.mmorpg.crafting.ForgeRecipes.all()) {int tier=com.mmorpg.crafting.ForgeProgression.required(recipe);if(tier>0&&(forgeBlueprints&(1<<(tier-1)))!=0)forgeRecipes.add(recipe.id());}
            forgeBlueprints=0;
        }
        minesUnlocked = Math.max(1, t.getIntOr("minesUnlocked", 1));
        plainsUnlocked = Math.max(1, t.getIntOr("plainsUnlocked", 1));
        // joueurs d'avant les quetes d'evolution : ils gardent le palier deja atteint
        evolution = t.getIntOr("evolution", -1);
        if (evolution < 0) evolution = PlayerClass.tier(level);
        gold = t.getLongOr("gold", 0);
        dbVersion = t.getLongOr("dbVersion", 0);
        xpCharmSeconds = Math.clamp(t.getIntOr("xpCharmSeconds", 0), 0, 1800);
        luckCharmSeconds = Math.clamp(t.getIntOr("luckCharmSeconds", 0), 0, 1800);
        trackedQuest = t.getStringOr("trackedQuest", "");
        questGuidanceMuted=t.getBooleanOr("questGuidanceMuted",false);
        questGivers.clear();questGivers.putAll(t.read("questGivers",Codec.unboundedMap(Codec.STRING,CompoundTag.CODEC)).orElse(Map.of()));
        mounts.clear();
        for (String m : t.read("mounts", Codec.STRING.listOf()).orElse(List.of())) mounts.add(com.mmorpg.mount.MountType.migrate(m));
        lastMount = t.getStringOr("lastMount", "");
        activeMount = t.getStringOr("activeMount", "");
        activeQuests.clear();
        t.read("activeQuests", Codec.unboundedMap(Codec.STRING, Codec.INT.listOf())).orElse(Map.of()).forEach((k, v) -> {
            int[] arr = new int[v.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = v.get(i);
            activeQuests.put(k, arr);
        });
        completedQuests.clear();
        completedQuests.addAll(t.read("completedQuests", Codec.STRING.listOf()).orElse(List.of()));
        dailyQuests.clear();
        dailyQuests.putAll(t.read("dailyQuests", Codec.unboundedMap(Codec.STRING, Codec.LONG)).orElse(Map.of()));
        dirty = true;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("rpg", CompoundTag.CODEC, save());
    }

    @Override
    public void deserialize(ValueInput input) {
        input.read("rpg", CompoundTag.CODEC).ifPresent(this::load);
    }
}
