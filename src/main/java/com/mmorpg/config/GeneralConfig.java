package com.mmorpg.config;

/** Reglages generaux du serveur (fichier config/mmorpg/general.json). */
public class GeneralConfig {
    /** Version du fichier (migrations automatiques ; ne pas modifier). */
    public int configVersion = 0;
    public static final int CURRENT_VERSION = 2;
    /** Multiplicateur global d'experience. */
    public double xpMultiplier = 1.0;
    /** Multiplicateur global des chances de butin. */
    public double dropMultiplier = 1.0;
    /** Donne un niveau et des statistiques RPG aux monstres vanilla (zombies, squelettes...). */
    public boolean scaleVanillaMobs = true;
    /** "player" : niveau proche de celui du joueur le plus proche ; "distance" : selon la distance au point d'apparition. */
    public String vanillaScalingMode = "player";
    public int blocksPerZoneLevel = 150;
    public int netherLevelBonus = 25;
    public int endLevelBonus = 50;
    /** Pourcentage de l'experience du niveau en cours perdue a la mort. */
    public double deathXpLossPercent = 5.0;
    /** Les competences peuvent-elles blesser les autres joueurs (si le PvP du serveur est actif ; jamais les membres du groupe). */
    public boolean skillsHitPlayers = true;
    /** Apparition naturelle des monstres RPG autour des joueurs. */
    public boolean customSpawning = true;
    public int spawnIntervalTicks = 80;
    public int maxCustomMobsNearPlayer = 8;
    public double goldDropChance = 0.65;
    public double upgradeStoneDropChance = 0.03;
    /** Chance qu'un monstre (niveau 10 et plus) lache un trefle d'amelioration. */
    public double cloverDropChance = 0.012;
    public double potionDropChance = 0.04;
    public boolean announceBossKills = true;
    public boolean announceEvolutions = true;
    /** Ouvre automatiquement l'ecran de choix de classe a la premiere connexion. */
    public boolean openClassSelectionOnJoin = true;
}
