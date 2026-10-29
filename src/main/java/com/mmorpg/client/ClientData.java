package com.mmorpg.client;

import com.mmorpg.rpg.BuffType;
import com.mmorpg.rpg.ClientView;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import com.mmorpg.rpg.StatBlock;
import com.mmorpg.rpg.StatCalculator;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Copie cote client des donnees RPG du joueur local (recue du serveur). */
public final class ClientData {
    public static final PlayerData DATA = new PlayerData();
    public static StatBlock stats = new StatBlock();
    public static long xpToNext = 100;
    public static int skillPoints = 0;
    public static float hp = 1, maxHp = 1, mana = 1, maxMana = 1;
    /** Recharges : competence -> tick client de fin. */
    public static final Map<String, long[]> COOLDOWNS = new HashMap<>();
    public static final Map<BuffType, long[]> BUFFS = new EnumMap<>(BuffType.class);
    public static CompoundTag mobInfo = new CompoundTag();
    /** Quetes en cours (avec progression), recues a chaque synchronisation. */
    public static net.minecraft.nbt.ListTag quests = new net.minecraft.nbt.ListTag();
    /** Etat du groupe (membres, invitation en attente). */
    public static CompoundTag party = new CompoundTag();
    public static boolean received = false;
    public static boolean creativeAllowed;public static int guardBroken;
    public static CompoundTag features=new CompoundTag();
    public static CompoundTag arenaState=new CompoundTag();
    public static long arenaUpdated;
    public static String backend="";
    /** Essai de classe en cours (indicateur du HUD). */
    public static boolean trialActive;
    public static int trialClass;
    public static int trialLevel;
    public static long trialSince;

    private ClientData() {
    }

    public static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }

    public static void load(CompoundTag t) {
        DATA.load(t);features=t.getCompoundOrEmpty("features");
        creativeAllowed=t.getBooleanOr("creativeAllowed",false);guardBroken=t.getIntOr("guardBroken",0);
        backend=t.getStringOr("backend","");
        List<Double> raw = t.read("statBlock", Codec.DOUBLE.listOf()).orElse(List.of());
        double[] arr = new double[raw.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = raw.get(i);
        stats = StatBlock.fromRaw(arr);
        xpToNext = t.getLongOr("xpToNext", 100);
        skillPoints = t.getIntOr("skillPoints", 0);
        long now = now();
        COOLDOWNS.clear();
        CompoundTag cds = t.getCompoundOrEmpty("cooldowns");
        for (String k : cds.keySet()) {
            int rem = cds.getIntOr(k, 0);
            COOLDOWNS.put(k, new long[]{now + rem, rem});
        }
        BUFFS.clear();
        CompoundTag buffs = t.getCompoundOrEmpty("buffs");
        for (String k : buffs.keySet()) {
            try {
                BuffType type = BuffType.valueOf(k);
                CompoundTag b = buffs.getCompoundOrEmpty(k);
                BUFFS.put(type, new long[]{now + b.getIntOr("rem", 0), b.getIntOr("total", 1)});
            } catch (IllegalArgumentException ignored) {
            }
        }
        quests = t.getListOrEmpty("quests");
        maxHp = (float) stats.get(Stat.MAX_HP);
        maxMana = (float) stats.get(Stat.MAX_MANA);
        received = true;
        ClientView.playerClass = DATA.playerClass;
        ClientView.level = DATA.level;
        ClientView.jobLevel = id -> com.mmorpg.job.Job.levelFor(DATA.jobXp.getOrDefault(id, 0L));
        ClientView.setPieceCounter = id -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.player == null ? 0 : StatCalculator.setPieces(mc.player, id);
        };
    }

    /** Fraction de recharge restante (1 = vient d'etre lancee, 0 = prete). */
    public static float cooldownFraction(String skill) {
        long[] cd = COOLDOWNS.get(skill);
        if (cd == null) return 0;
        long rem = cd[0] - now();
        if (rem <= 0) return 0;
        return Math.min(1f, rem / (float) Math.max(1, cd[1]));
    }

    public static float cooldownSeconds(String skill) {
        long[] cd = COOLDOWNS.get(skill);
        if (cd == null) return 0;
        return Math.max(0, (cd[0] - now()) / 20f);
    }

    public static void reset() {
        backend="";
        DATA.load(new PlayerData().save());
        stats = new StatBlock();
        COOLDOWNS.clear();
        BUFFS.clear();
        quests = new net.minecraft.nbt.ListTag();
        party = new CompoundTag();
        received = false;
        trialActive = false;
    }
}
