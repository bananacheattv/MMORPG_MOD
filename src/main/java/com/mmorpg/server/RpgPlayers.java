package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.pet.PetType;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.Buff;
import com.mmorpg.rpg.BuffType;
import com.mmorpg.rpg.LevelSystem;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.PublicPlayerData;
import com.mmorpg.rpg.Stat;
import com.mmorpg.rpg.StatBlock;
import com.mmorpg.rpg.StatCalculator;
import com.mmorpg.skill.Skill;
import com.mmorpg.skill.Skills;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Logique serveur des joueurs : statistiques, experience, niveaux, regeneration et synchronisation. */
public final class RpgPlayers {
    private static final net.minecraft.resources.Identifier SPEED_ID = MMORPG.id("rpg_speed");
    private static final net.minecraft.resources.Identifier ATK_SPEED_ID = MMORPG.id("rpg_attack_speed");
    private static final Map<UUID, Integer> EQUIP_HASH = new HashMap<>();
    private static final Map<UUID, float[]> LAST_VITALS = new HashMap<>();
    private static final Map<UUID, Integer> QUEST_SIG = new HashMap<>();

    private RpgPlayers() {
    }

    public static PlayerData get(Player player) {
        return player.getData(ModAttachments.PLAYER);
    }

    // ================================================================== statistiques

    public static void recompute(ServerPlayer player) {
        PlayerData d = get(player);
        StatBlock old = d.stats;
        double oldMax = old.get(Stat.MAX_HP);
        d.stats = StatCalculator.compute(player, d);
        double max = d.stats.get(Stat.MAX_HP);
        double maxMana = d.stats.get(Stat.MAX_MANA);
        if (d.hp < 0) {
            d.hp = (float) max;
        } else if (oldMax > 0 && Math.abs(oldMax - max) > 0.01) {
            // conserve le pourcentage de vie lors d'un changement d'equipement
            d.hp = (float) Math.min(max, d.hp * (max / oldMax));
        }
        d.hp = (float) Math.min(d.hp, max);
        if (d.mana < 0) d.mana = (float) maxMana;
        d.mana = (float) Math.min(d.mana, maxMana);

        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, d.stats.get(Stat.SPEED) / 100.0);
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATK_SPEED_ID, d.stats.get(Stat.ATK_SPEED) / 100.0);
        EQUIP_HASH.put(player.getUUID(), StatCalculator.equipmentHash(player));
        d.dirty = true;
    }

    private static void applyModifier(AttributeInstance inst, net.minecraft.resources.Identifier id, double value) {
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - value) < 1e-4) return;
        inst.removeModifier(id);
        if (Math.abs(value) > 1e-4) {
            inst.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void recomputeAndSync(ServerPlayer player) {
        recompute(player);
        sync(player);
    }

    // ================================================================== synchronisation

    public static void sync(ServerPlayer player) {
        player.refreshTabListName();
        PlayerData d = get(player);
        CompoundTag t = d.save();
        t.store("stats", com.mojang.serialization.Codec.DOUBLE.listOf(), toList(d.stats.raw()));
        t.putLong("xpToNext", LevelSystem.xpToNext(d.level));
        t.putInt("skillPoints", d.availableSkillPoints());
        long now = player.level().getGameTime();
        CompoundTag cds = new CompoundTag();
        for (Map.Entry<String, Long> e : d.cooldowns.entrySet()) {
            long rem = e.getValue() - now;
            if (rem > 0) cds.putInt(e.getKey(), (int) rem);
        }
        t.put("cooldowns", cds);
        CompoundTag buffs = new CompoundTag();
        for (Buff b : d.buffs.values()) {
            CompoundTag bt = new CompoundTag();
            bt.putInt("rem", b.remaining(now));
            bt.putInt("total", b.totalTicks);
            buffs.put(b.type.name(), bt);
        }
        t.put("buffs", buffs);
        t.put("quests", QuestManager.activeTag(player, d));
        Net.toPlayer(player, new Payloads.SyncPlayer(t));
        d.dirty = false;
        sendVitals(player, true);
        updatePublic(player);
    }

    private static List<Double> toList(double[] raw) {
        List<Double> l = new ArrayList<>(raw.length);
        for (double v : raw) l.add(v);
        return l;
    }

    public static void sendVitals(ServerPlayer player, boolean force) {
        PlayerData d = get(player);
        float[] v = {d.hp, (float) d.stats.get(Stat.MAX_HP), d.mana, (float) d.stats.get(Stat.MAX_MANA)};
        float[] last = LAST_VITALS.get(player.getUUID());
        if (!force && last != null && Math.abs(last[0] - v[0]) < 0.05f && Math.abs(last[2] - v[2]) < 0.05f
                && last[1] == v[1] && last[3] == v[3]) {
            return;
        }
        LAST_VITALS.put(player.getUUID(), v);
        Net.toPlayer(player, new Payloads.Vitals(v[0], v[1], v[2], v[3]));
    }

    public static void updatePublic(ServerPlayer player) {
        PlayerData d = get(player);
        PublicPlayerData pub = new PublicPlayerData();
        pub.playerClass = d.playerClass.ordinal();
        pub.level = d.level;
        for (Cosmetic.Category c : Cosmetic.Category.values()) {
            String id = d.equippedCosmetics.get(c);
            if (id != null && !id.isEmpty()) pub.cosmetics.add(id);
        }
        double max = d.stats.get(Stat.MAX_HP);
        pub.hpFraction = max > 0 ? (float) (d.hp / max) : 1;
        PublicPlayerData old = player.getExistingDataOrNull(ModAttachments.PUBLIC);
        if (!pub.sameAs(old)) {
            player.setData(ModAttachments.PUBLIC, pub);
        }
    }

    // ================================================================== boucle

    public static void tick(ServerPlayer player) {
        PlayerData d = get(player);
        long now = player.level().getGameTime();
        if (d.stats.get(Stat.MAX_HP) <= 0 || d.hp < 0) {
            recompute(player);
        }
        if (now % 20 == 0 && (d.xpCharmSeconds > 0 || d.luckCharmSeconds > 0)) {
            d.xpCharmSeconds = Math.max(0, d.xpCharmSeconds - 1);
            d.luckCharmSeconds = Math.max(0, d.luckCharmSeconds - 1);
            d.dirty = true;
        }
        // effets expires
        boolean buffsChanged = false;
        Iterator<Buff> it = d.buffs.values().iterator();
        while (it.hasNext()) {
            Buff b = it.next();
            if (b.expiresAt <= now) {
                it.remove();
                buffsChanged = true;
            } else if (b.type == BuffType.SACRED_AURA && now % 20 == 0) {
                heal(player, d.stats.get(Stat.MAX_HP) * b.magnitude, true);
            }
        }
        if (buffsChanged) recompute(player);
        // changement d'equipement
        if (now % 5 == 0) {
            Integer h = EQUIP_HASH.get(player.getUUID());
            if (h == null || h != StatCalculator.equipmentHash(player)) {
                recompute(player);
            }
        }
        // regeneration
        if (player.isAlive()) {
            double maxHp = d.stats.get(Stat.MAX_HP);
            double maxMana = d.stats.get(Stat.MAX_MANA);
            boolean inCombat = player.getLastHurtByMobTimestamp() > 0 && player.tickCount - player.getLastHurtByMobTimestamp() < 100;
            double hpRegen = d.stats.get(Stat.HP_REGEN) * (inCombat ? 0.5 : 1.0) / 20.0;
            d.hp = (float) Math.min(maxHp, d.hp + hpRegen);
            d.mana = (float) Math.min(maxMana, d.mana + d.stats.get(Stat.MANA_REGEN) / 20.0);
            syncVanillaHealth(player);
        }
        // pieces d'or ramassees -> porte-monnaie ; suivi des objectifs de collecte
        if (now % 10 == 0) {
            depositCoins(player);
        }
        if (now % 40 == 0 && !d.activeQuests.isEmpty()) {
            int sig = QuestManager.collectSignature(player, d);
            Integer old = QUEST_SIG.put(player.getUUID(), sig);
            if (old == null || old != sig) d.dirty = true;
        }
        if (d.dirty) {
            sync(player);
        } else if (now % 4 == 0) {
            sendVitals(player, false);
            if (now % 20 == 0) updatePublic(player);
        }
        if (now % 20 == 0) {
            PetManager.maintain(player);
        }
    }

    /** La sante vanilla suit le pourcentage de PV RPG (les coeurs vanilla sont masques). */
    public static void syncVanillaHealth(ServerPlayer player) {
        PlayerData d = get(player);
        double max = d.stats.get(Stat.MAX_HP);
        if (max <= 0) return;
        float desired = (float) Math.max(0.5, player.getMaxHealth() * d.hp / max);
        if (d.hp <= 0) return;
        if (Math.abs(player.getHealth() - desired) > 0.01f) {
            player.setHealth(desired);
        }
    }

    // ================================================================== vie / mana

    public static void heal(ServerPlayer player, double amount, boolean showText) {
        PlayerData d = get(player);
        double max = d.stats.get(Stat.MAX_HP);
        float before = d.hp;
        d.hp = (float) Math.min(max, d.hp + Math.max(0, amount));
        if (showText && d.hp - before >= 1) {
            CombatTexts.send(player, player.getX(), player.getY() + player.getBbHeight() + 0.3, player.getZ(), d.hp - before, Payloads.CombatText.HEAL);
        }
        syncVanillaHealth(player);
    }

    public static void healPercent(ServerPlayer player, double pct) {
        heal(player, get(player).stats.get(Stat.MAX_HP) * pct, true);
    }

    public static void restoreMana(ServerPlayer player, double amount) {
        PlayerData d = get(player);
        d.mana = (float) Math.min(d.stats.get(Stat.MAX_MANA), d.mana + amount);
        sendVitals(player, true);
    }

    public static void restoreManaPercent(ServerPlayer player, double pct) {
        PlayerData d = get(player);
        double amount = d.stats.get(Stat.MAX_MANA) * pct;
        restoreMana(player, amount);
        CombatTexts.send(player, player.getX(), player.getY() + player.getBbHeight() + 0.3, player.getZ(), (float) amount, Payloads.CombatText.MANA);
    }

    public static void fullRestore(ServerPlayer player) {
        PlayerData d = get(player);
        d.hp = (float) d.stats.get(Stat.MAX_HP);
        d.mana = (float) d.stats.get(Stat.MAX_MANA);
        syncVanillaHealth(player);
        sendVitals(player, true);
    }

    // ================================================================== experience

    public static void giveXp(ServerPlayer player, long amount, boolean fromKill) {
        PlayerData d = get(player);
        if (!d.playerClass.isPlayable() || d.level >= LevelSystem.MAX_LEVEL || amount <= 0) return;
        amount = Math.max(1, Math.round(amount * ConfigManager.general().xpMultiplier * (d.xpCharmSeconds > 0 ? 1.25 : 1.0)));
        d.xp += amount;
        CombatTexts.send(player, player.getX(), player.getY() + player.getBbHeight() + 0.6, player.getZ(), amount, Payloads.CombatText.XP);
        boolean leveled = false;
        int oldLevel = d.level;
        while (d.level < LevelSystem.MAX_LEVEL && d.xp >= LevelSystem.xpToNext(d.level)) {
            d.xp -= LevelSystem.xpToNext(d.level);
            d.level++;
            d.attributePoints += LevelSystem.ATTRIBUTE_POINTS_PER_LEVEL;
            leveled = true;
        }
        if (d.level >= LevelSystem.MAX_LEVEL) d.xp = 0;
        if (leveled) onLevelUp(player, oldLevel, d.level);
        d.dirty = true;
    }

    private static void onLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        PlayerData d = get(player);
        List<Skill> unlocked = d.unlockSkills();
        recompute(player);
        fullRestore(player);
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 50, 0.4, 0.8, 0.4, 0.4);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
        int oldTier = PlayerClass.tier(oldLevel);
        int newTier = PlayerClass.tier(newLevel);
        if (newTier > oldTier) {
            String title = d.playerClass.title(newLevel);
            Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.EVOLUTION, "ÉVOLUTION !", "Vous devenez " + title, d.playerClass.color));
            level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 80, 0.6, 1.2, 0.6, 0.25);
            if (ConfigManager.general().announceEvolutions) {
                player.level().getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("✦ ").withColor(0xFFD040)
                                .append(player.getDisplayName())
                                .append(Component.literal(" a évolué en ").withColor(0xE0D8C0))
                                .append(Component.literal(title).withColor(d.playerClass.color & 0xFFFFFF))
                                .append(Component.literal(" (niveau " + newLevel + ") !").withColor(0xE0D8C0)), false);
            }
        } else {
            Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.LEVEL_UP, "NIVEAU " + newLevel,
                    "+" + (newLevel - oldLevel) * LevelSystem.ATTRIBUTE_POINTS_PER_LEVEL + " points d'attribut", 0xFFD040));
        }
        for (Skill s : unlocked) {
            player.sendSystemMessage(Component.literal("✦ Nouvelle compétence débloquée : ").withColor(0xE8C060)
                    .append(Component.literal(s.name).withColor(d.playerClass.color & 0xFFFFFF)));
        }
        for (Cosmetic c : Cosmetic.values()) {
            if (c.unlockLevel > 0 && c.unlockLevel <= newLevel && d.cosmetics.add(c.id)) {
                player.sendSystemMessage(Component.literal("✦ Cosmétique débloqué : ").withColor(0xE8C060)
                        .append(Component.literal(c.label).withColor(c.color & 0xFFFFFF)));
            }
        }
    }

    public static void setLevel(ServerPlayer player, int level) {
        PlayerData d = get(player);
        int old = d.level;
        d.level = Math.max(1, Math.min(LevelSystem.MAX_LEVEL, level));
        d.xp = 0;
        int spent = 0;
        for (int a : d.allocated) spent += a;
        d.attributePoints = Math.max(0, LevelSystem.attributePointsAt(d.level) - spent);
        if (spent > LevelSystem.attributePointsAt(d.level)) d.resetAttributes();
        if (d.level < old) {
            // retire les competences au-dessus du niveau
            d.skillRanks.entrySet().removeIf(e -> {
                Skill s = Skills.get(e.getKey());
                return s == null || s.unlockLevel > d.level;
            });
            for (int i = 0; i < d.skillBar.length; i++) {
                if (!d.skillRanks.containsKey(d.skillBar[i])) d.skillBar[i] = "";
            }
            while (d.spentSkillPoints() > LevelSystem.skillPointsAt(d.level)) {
                d.skillRanks.replaceAll((k, v) -> Math.max(1, v - 1));
            }
        }
        if (d.level > old) onLevelUp(player, old, d.level);
        recomputeAndSync(player);
    }

    // ================================================================== actions joueur

    public static void selectClass(ServerPlayer player, PlayerClass cls) {
        PlayerData d = get(player);
        if (!cls.isPlayable() || d.playerClass.isPlayable()) return;
        d.playerClass = cls;
        d.skillRanks.clear();
        java.util.Arrays.fill(d.skillBar, "");
        d.unlockSkills();
        d.attributePoints = LevelSystem.attributePointsAt(d.level);
        java.util.Arrays.fill(d.allocated, 0);
        for (Cosmetic c : Cosmetic.values()) {
            if (c.unlockLevel > 0 && c.unlockLevel <= d.level) d.cosmetics.add(c.id);
        }
        if (!d.classChosenOnce) {
            d.classChosenOnce = true;
            giveStarterKit(player, cls);
        }
        d.hp = -1;
        d.mana = -1;
        recompute(player);
        fullRestore(player);
        sync(player);
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.2f);
        Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.EVOLUTION, cls.label.toUpperCase(java.util.Locale.ROOT),
                "Votre aventure commence !", cls.color));
    }

    private static void giveStarterKit(ServerPlayer player, PlayerClass cls) {
        String weapon = switch (cls) {
            case GUERRIER -> "epee_recrue";
            case MAGE -> "baton_apprenti";
            case ARCHER -> "arc_chasseur";
            default -> "masse_garde";
        };
        give(player, ModItems.get(weapon), 1);
        give(player, ModItems.POTION_SOIN_MINEURE.get(), 5);
        give(player, ModItems.POTION_MANA_MINEURE.get(), 3);
        give(player, ModItems.PARCHEMIN_TELEPORTATION.get(), 2);
        give(player, ModItems.PIECE_OR.get(), 10);
        player.sendSystemMessage(Component.literal("Vous avez reçu votre équipement de départ. Appuyez sur ").withColor(0xE0D8C0)
                .append(Component.literal("[M]").withColor(0xFFD040))
                .append(Component.literal(" pour ouvrir le menu du personnage.").withColor(0xE0D8C0)));
    }

    public static void give(ServerPlayer player, Item item, int count) {
        if (item == null) return;
        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    public static void allocate(ServerPlayer player, int attr, int amount) {
        PlayerData d = get(player);
        if (attr < 0 || attr >= Stat.PRIMARIES.length || amount <= 0 || !d.playerClass.isPlayable()) return;
        amount = Math.min(amount, d.attributePoints);
        if (amount <= 0) return;
        d.attributePoints -= amount;
        d.allocated[attr] += amount;
        recomputeAndSync(player);
    }

    public static void upgradeSkill(ServerPlayer player, String id) {
        PlayerData d = get(player);
        Skill s = Skills.get(id);
        if (s == null || !d.isSkillUnlocked(s)) return;
        int rank = d.skillRank(id);
        if (rank >= Skill.MAX_RANK || d.availableSkillPoints() <= 0) return;
        d.skillRanks.put(id, rank + 1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
        recomputeAndSync(player);
    }

    public static void assignSkill(ServerPlayer player, int slot, String id) {
        PlayerData d = get(player);
        if (slot < 0 || slot >= PlayerData.SKILL_SLOTS) return;
        if (id.isEmpty()) {
            d.skillBar[slot] = "";
        } else {
            Skill s = Skills.get(id);
            if (s == null || s.passive || !d.isSkillUnlocked(s)) return;
            for (int i = 0; i < d.skillBar.length; i++) {
                if (d.skillBar[i].equals(id)) d.skillBar[i] = d.skillBar[slot];
            }
            d.skillBar[slot] = id;
        }
        sync(player);
    }

    public static void equipCosmetic(ServerPlayer player, int category, String id) {
        PlayerData d = get(player);
        Cosmetic.Category[] cats = Cosmetic.Category.values();
        if (category < 0 || category >= cats.length) return;
        Cosmetic.Category cat = cats[category];
        if (id.isEmpty()) {
            d.equippedCosmetics.remove(cat);
        } else {
            Cosmetic c = Cosmetic.byId(id);
            if (c == null || c.category != cat || !d.cosmetics.contains(id)) return;
            d.equippedCosmetics.put(cat, id);
        }
        sync(player);
    }

    public static void unlockPet(ServerPlayer player, PetType pet) {
        PlayerData d = get(player);
        if (d.petXp.containsKey(pet.id)) {
            PetManager.addPetXp(player, pet.id, 400);
            player.sendSystemMessage(Component.literal("Votre " + pet.label + " gagne 400 points d'expérience.").withColor(0xE8C060));
        } else {
            d.petXp.put(pet.id, 0);
            Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.DISCOVERY, "NOUVEAU FAMILIER", pet.label, pet.color));
            player.level().playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1f, 0.8f);
            if (d.activePet.isEmpty()) {
                PetManager.setActive(player, pet.id);
            }
        }
        recomputeAndSync(player);
    }

    public static void unlockRandomCosmetic(ServerPlayer player) {
        PlayerData d = get(player);
        List<Cosmetic> locked = new ArrayList<>();
        for (Cosmetic c : Cosmetic.values()) {
            if (c.unlockLevel < 0 && !d.cosmetics.contains(c.id)) locked.add(c);
        }
        if (locked.isEmpty()) {
            give(player, ModItems.PIECE_OR.get(), 50);
            player.sendSystemMessage(Component.literal("Vous possédez déjà tous les cosmétiques ! Vous recevez 50 pièces d'or.").withColor(0xE8C060));
            return;
        }
        Cosmetic c = locked.get(player.getRandom().nextInt(locked.size()));
        d.cosmetics.add(c.id);
        Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.DISCOVERY, "COSMÉTIQUE OBTENU", c.label, c.color));
        sync(player);
    }

    public static void openClassSelection(ServerPlayer player) {
        Net.toPlayer(player, S2COpenScreen.classSelect());
    }

    // ================================================================== cycle de vie

    public static void onLogin(ServerPlayer player) {
        PlayerData d = get(player);
        d.unlockSkills();
        for (Cosmetic c : Cosmetic.values()) {
            if (c.unlockLevel > 0 && c.unlockLevel <= d.level) d.cosmetics.add(c.id);
        }
        d.equippedCosmetics.entrySet().removeIf(e -> {
            Cosmetic c = Cosmetic.byId(e.getValue());
            return c == null || c.category != e.getKey() || !d.cosmetics.contains(c.id);
        });
        recompute(player);
        if (d.hp <= 0) d.hp = (float) d.stats.get(Stat.MAX_HP);
        sync(player);
        BestiaryInfo.sendTo(player);
        PartyManager.sync(player);
        if (!d.playerClass.isPlayable() && ConfigManager.general().openClassSelectionOnJoin) {
            openClassSelection(player);
        }
    }

    public static void onRespawn(ServerPlayer player) {
        PlayerData d = get(player);
        d.buffs.clear();
        d.cooldowns.clear();
        recompute(player);
        fullRestore(player);
        sync(player);
    }

    public static void onDeath(ServerPlayer player) {
        PlayerData d = get(player);
        d.hp = 0;
        double loss = ConfigManager.general().deathXpLossPercent;
        if (loss > 0 && d.playerClass.isPlayable() && d.level < LevelSystem.MAX_LEVEL) {
            long lost = Math.round(LevelSystem.xpToNext(d.level) * loss / 100.0);
            lost = Math.min(lost, d.xp);
            if (lost > 0) {
                d.xp -= lost;
                player.sendSystemMessage(Component.literal("Vous avez perdu " + lost + " points d'expérience.").withColor(0xFF6060));
            }
        }
        PetManager.despawn(player);
    }

    public static void onLogout(ServerPlayer player) {
        PetManager.despawn(player);
        PartyManager.onLogout(player);
        ClickInteractions.forget(player);
        QUEST_SIG.remove(player.getUUID());
        EQUIP_HASH.remove(player.getUUID());
        LAST_VITALS.remove(player.getUUID());
    }

    // ================================================================== or

    public static void addGold(ServerPlayer player, long amount, boolean showText) {
        if (amount <= 0) return;
        PlayerData d = get(player);
        d.gold += amount;
        d.dirty = true;
        if (showText) {
            CombatTexts.send(player, player.getX(), player.getY() + player.getBbHeight() + 0.45, player.getZ(), amount, Payloads.CombatText.GOLD);
        }
    }

    public static boolean takeGold(ServerPlayer player, long amount) {
        PlayerData d = get(player);
        if (amount <= 0) return true;
        if (d.gold < amount) return false;
        d.gold -= amount;
        d.dirty = true;
        return true;
    }

    /** Verse les pieces d'or de l'inventaire dans le porte-monnaie. */
    public static void depositCoins(ServerPlayer player) {
        var inv = player.getInventory();
        long total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.PIECE_OR.get())) {
                total += s.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (total > 0) addGold(player, total, true);
    }

    // ================================================================== effets

    public static void addBuff(ServerPlayer player, BuffType type, int ticks, double magnitude, double magnitude2) {
        PlayerData d = get(player);
        long now = player.level().getGameTime();
        d.buffs.put(type, new Buff(type, magnitude, magnitude2, now + ticks, ticks));
        recompute(player);
        d.dirty = true;
    }

    public static Buff buff(Player player, BuffType type) {
        return get(player).buffs.get(type);
    }
}
