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
    /** Porte-monnaie (pieces d'or). */
    public long gold = 0;
    /** Quetes en cours : progression de chaque objectif. */
    public final Map<String, int[]> activeQuests = new LinkedHashMap<>();
    public final Set<String> completedQuests = new HashSet<>();
    /** Quetes journalieres : jour (numero de jour du monde) de la derniere realisation. */
    public final Map<String, Long> dailyQuests = new HashMap<>();

    // --- transient (non sauvegarde)
    public StatBlock stats = new StatBlock();
    public final Map<String, Long> cooldowns = new HashMap<>();
    public final Map<BuffType, Buff> buffs = new EnumMap<>(BuffType.class);
    public UUID petEntity;
    public boolean dirty = true;
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
        return Math.max(0, LevelSystem.skillPointsAt(level) - spentSkillPoints());
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
        t.store("petHappiness", STR_INT_MAP, petHappiness);
        t.store("cosmetics", Codec.STRING.listOf(), new ArrayList<>(cosmetics));
        Map<String, String> eq = new HashMap<>();
        equippedCosmetics.forEach((k, v) -> eq.put(k.name(), v));
        t.store("equipped", STR_STR_MAP, eq);
        t.store("waypoints", UUIDUtil.CODEC.listOf(), new ArrayList<>(waypoints));
        t.store("kills", STR_INT_MAP, kills);
        t.putBoolean("chosen", classChosenOnce);
        t.putLong("gold", gold);
        Map<String, List<Integer>> aq = new LinkedHashMap<>();
        activeQuests.forEach((k, v) -> {
            List<Integer> l = new ArrayList<>();
            for (int x : v) l.add(x);
            aq.put(k, l);
        });
        t.store("activeQuests", Codec.unboundedMap(Codec.STRING, Codec.INT.listOf()), aq);
        t.store("completedQuests", Codec.STRING.listOf(), new ArrayList<>(completedQuests));
        t.store("dailyQuests", Codec.unboundedMap(Codec.STRING, Codec.LONG), dailyQuests);
        return t;
    }

    public void load(CompoundTag t) {
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
        classChosenOnce = t.getBooleanOr("chosen", playerClass != PlayerClass.NONE);
        gold = t.getLongOr("gold", 0);
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
