package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.pet.PetType;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.rpg.LevelSystem;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalDate;
import java.util.Map;

/** Quetes : disponibilite, acceptation, progression, remise et recompenses. Toujours valide cote serveur. */
public final class QuestManager {
    public static final int MAX_ACTIVE = 10;

    private QuestManager() {
    }

    public static long today() {
        return LocalDate.now().toEpochDay();
    }

    public static QuestDef def(String id) {
        return ConfigManager.quests().get(id);
    }

    public static boolean isAvailable(PlayerData d, String id, QuestDef q, String group) {
        if (q == null || d.activeQuests.containsKey(id) || d.level < q.minLevel) return false;
        if (!q.giver.isEmpty() && !q.giver.equalsIgnoreCase(group)) return false;
        for (String r : q.requires) {
            if (!d.completedQuests.contains(r)) return false;
        }
        if (q.daily) {
            Long done = d.dailyQuests.get(id);
            return done == null || done != today();
        }
        return !d.completedQuests.contains(id);
    }

    public static boolean matchesNpc(QuestDef q, NpcEntity npc) {
        return (q.npc == null || q.npc.isEmpty() || q.npc.equals(npc.getUUID().toString()))
                && (q.giver.isEmpty() || q.giver.equalsIgnoreCase(npc.group()));
    }

    private static int count(Inventory inv, Item item) {
        int n = 0;
        for (ItemStack s : inv) {
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    public static int progress(ServerPlayer p, PlayerData d, String id, int index) {
        QuestDef q = def(id);
        int[] prog = d.activeQuests.get(id);
        if (q == null || prog == null || index >= q.objectives.size()) return 0;
        QuestDef.Objective o = q.objectives.get(index);
        return switch (o.type) {
            case "collect" -> {
                Item item = ForgeRecipes.resolveItem(o.target);
                yield item == null || item == Items.AIR ? 0 : Math.min(o.count, count(p.getInventory(), item));
            }
            case "level" -> Math.min(o.count, d.level);
            default -> index < prog.length ? Math.min(o.count, prog[index]) : 0;
        };
    }

    public static boolean completable(ServerPlayer p, PlayerData d, String id) {
        QuestDef q = def(id);
        if (q == null || !d.activeQuests.containsKey(id)) return false;
        for (int i = 0; i < q.objectives.size(); i++) {
            if (progress(p, d, id, i) < q.objectives.get(i).count) return false;
        }
        Map<Item, Long> needed = new java.util.HashMap<>();
        for (QuestDef.Objective o : q.objectives) {
            if ("collect".equals(o.type)) needed.merge(ForgeRecipes.resolveItem(o.target), (long) o.count, Long::sum);
        }
        for (var e : needed.entrySet()) {
            if (e.getKey() == null || e.getKey() == Items.AIR || count(p.getInventory(), e.getKey()) < e.getValue()) return false;
        }
        return true;
    }

    public static long rewardXp(QuestDef q, int playerLevel) {
        return q.rewards.xp + Math.round(LevelSystem.xpToNext(playerLevel) * q.rewards.xpPercentOfLevel / 100.0);
    }

    // ================================================================== donnees envoyees au client

    public static CompoundTag questTag(ServerPlayer p, PlayerData d, String id, QuestDef q) {
        CompoundTag t = new CompoundTag();
        t.putString("id", id);
        t.putString("name", q.name);
        t.putString("desc", q.description);
        t.putString("giver", q.giver);
        if (q.npc != null && !q.npc.isEmpty()) {
            var npc = com.mmorpg.world.NpcDirectory.get(p.level().getServer()).entry(q.npc);
            if (npc != null) t.putString("giver", npc.getStringOr("name", "Maître des Quêtes") + " — " + npc.getStringOr("zone", "Sans zone"));
        }
        t.putBoolean("tracked", id.equals(d.trackedQuest));
        t.putInt("minLevel", q.minLevel);
        t.putBoolean("daily", q.daily);
        boolean active = d.activeQuests.containsKey(id);
        t.putBoolean("active", active);
        ListTag objs = new ListTag();
        for (int i = 0; i < q.objectives.size(); i++) {
            QuestDef.Objective o = q.objectives.get(i);
            CompoundTag ot = new CompoundTag();
            ot.putString("type", o.type);
            ot.putString("target", o.target);
            ot.putInt("count", o.count);
            ot.putInt("progress", active ? progress(p, d, id, i) : 0);
            objs.add(ot);
        }
        t.put("objectives", objs);
        t.putBoolean("completable", active && completable(p, d, id));
        CompoundTag rw = new CompoundTag();
        rw.putLong("xp", rewardXp(q, d.level));
        rw.putLong("gold", q.rewards.gold);
        ListTag items = new ListTag();
        for (QuestDef.ItemReward ir : q.rewards.items) {
            CompoundTag it = new CompoundTag();
            it.putString("item", ir.item);
            it.putInt("count", ir.count);
            items.add(it);
        }
        rw.put("items", items);
        rw.putString("pet", q.rewards.pet == null ? "" : q.rewards.pet);
        rw.putInt("evolution", q.rewards.evolution);
        t.put("rewards", rw);
        return t;
    }

    /** Quetes en cours, incluses dans la synchronisation du joueur (journal et suivi a l'ecran). */
    public static ListTag activeTag(ServerPlayer p, PlayerData d) {
        ListTag list = new ListTag();
        var ordered = new java.util.ArrayList<>(d.activeQuests.keySet());
        ordered.sort(java.util.Comparator.comparingInt(id -> id.equals(d.trackedQuest) ? 0 : completable(p, d, id) ? 1 : 2));
        for (String id : ordered) {
            QuestDef q = def(id);
            if (q != null) list.add(questTag(p, d, id, q));
        }
        return list;
    }

    /** Empreinte de la progression des objectifs "collecter" (detecte les changements d'inventaire). */
    public static int collectSignature(ServerPlayer p, PlayerData d) {
        int h = 0;
        for (Map.Entry<String, int[]> e : d.activeQuests.entrySet()) {
            QuestDef q = def(e.getKey());
            if (q == null) continue;
            for (int i = 0; i < q.objectives.size(); i++) {
                if ("collect".equals(q.objectives.get(i).type) || "level".equals(q.objectives.get(i).type)) {
                    h = h * 31 + progress(p, d, e.getKey(), i) + i;
                }
            }
        }
        return h;
    }

    public static void openGiver(ServerPlayer p, NpcEntity npc) {
        PlayerData d = RpgPlayers.get(p);
        if (!d.playerClass.isPlayable()) {
            RpgPlayers.openClassSelection(p);
            return;
        }
        ListTag available = new ListTag();
        ListTag active = new ListTag();
        for (Map.Entry<String, QuestDef> e : ConfigManager.quests().entrySet()) {
            QuestDef q = e.getValue();
            if (d.activeQuests.containsKey(e.getKey())) {
                if (matchesNpc(q, npc)) active.add(questTag(p, d, e.getKey(), q));
            } else if (matchesNpc(q, npc) && isAvailable(d, e.getKey(), q, npc.group())) {
                available.add(questTag(p, d, e.getKey(), q));
            }
        }
        CompoundTag t = new CompoundTag();
        t.putInt("npc", npc.getId());
        t.putString("name", npc.displayName());
        t.put("available", available);
        t.put("active", active);
        t.putInt("completed", d.completedQuests.size());
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.QUEST_GIVER, t));
        p.level().playSound(null, npc.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
    }

    private static NpcEntity nearGiver(ServerPlayer p, int npcId) {
        if (!p.isAlive() || p.isSpectator()) return null;
        Entity e = p.level().getEntity(npcId);
        if (e instanceof NpcEntity npc && npc.role() == NpcEntity.Role.QUETES && npc.distanceToSqr(p) < 64) return npc;
        return null;
    }

    // ================================================================== actions

    public static void accept(ServerPlayer p, String id, int npcId) {
        NpcEntity npc = nearGiver(p, npcId);
        PlayerData d = RpgPlayers.get(p);
        QuestDef q = def(id);
        if (npc == null || q == null || !matchesNpc(q, npc) || !isAvailable(d, id, q, npc.group())) return;
        if (d.activeQuests.size() >= MAX_ACTIVE) {
            p.sendOverlayMessage(Component.literal("Journal de quêtes plein (" + MAX_ACTIVE + " quêtes maximum).").withColor(0xFF5050));
            return;
        }
        d.activeQuests.put(id, new int[q.objectives.size()]);
        Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.QUEST, "QUÊTE ACCEPTÉE", q.name, 0xFFD040));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        d.dirty = true;
        openGiver(p, npc);
    }

    public static void track(ServerPlayer p, String id) {
        PlayerData d = RpgPlayers.get(p);
        if (!id.isEmpty() && !d.activeQuests.containsKey(id)) return;
        d.trackedQuest = id;
        d.dirty = true;
    }

    public static void abandon(ServerPlayer p, String id) {
        PlayerData d = RpgPlayers.get(p);
        QuestDef q = def(id);
        if (d.activeQuests.remove(id) != null) {
            if (id.equals(d.trackedQuest)) d.trackedQuest = "";
            p.sendSystemMessage(Component.literal("Quête abandonnée : " + (q != null ? q.name : id)).withColor(0xFF9070));
            d.dirty = true;
        }
    }

    public static void complete(ServerPlayer p, String id, int npcId) {
        NpcEntity npc = nearGiver(p, npcId);
        PlayerData d = RpgPlayers.get(p);
        QuestDef q = def(id);
        if (npc == null || q == null || !matchesNpc(q, npc) || !completable(p, d, id)) return;
        // objets a rapporter
        for (QuestDef.Objective o : q.objectives) {
            if (!"collect".equals(o.type)) continue;
            Item item = ForgeRecipes.resolveItem(o.target);
            int left = o.count;
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(item)) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
        }
        d.activeQuests.remove(id);
        if (id.equals(d.trackedQuest)) d.trackedQuest = "";
        if (q.daily) d.dailyQuests.put(id, today());
        else d.completedQuests.add(id);
        // recompenses
        long xp = rewardXp(q, d.level);
        if (q.rewards.gold > 0) RpgPlayers.addGold(p, q.rewards.gold, true);
        for (QuestDef.ItemReward ir : q.rewards.items) {
            Item item = ForgeRecipes.resolveItem(ir.item);
            if (item != null && item != Items.AIR && !CrateOnly.is(item)) RpgPlayers.give(p, item, ir.count);
        }
        // familiers et cosmetiques : uniquement dans les caisses et les Lucky Blocks (q.rewards.pet / cosmetic ignores)
        if (q.rewards.evolution > 0) RpgPlayers.unlockEvolution(p, q.rewards.evolution);
        Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.QUEST, "QUÊTE TERMINÉE", q.name, 0x60FF80));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.2f);
        if (xp > 0) RpgPlayers.giveXp(p, xp, false);
        d.dirty = true;
        openGiver(p, npc);
    }

    /** Progression des objectifs "vaincre" apres une victoire. */
    public static void onKill(ServerPlayer p, String mobKey, String typeId, boolean boss) {
        PlayerData d = RpgPlayers.get(p);
        if (d.activeQuests.isEmpty()) return;
        for (Map.Entry<String, int[]> e : d.activeQuests.entrySet()) {
            QuestDef q = def(e.getKey());
            if (q == null) continue;
            int[] prog = e.getValue();
            if (prog.length < q.objectives.size()) {
                prog = java.util.Arrays.copyOf(prog, q.objectives.size());
                e.setValue(prog);
            }
            boolean wasComplete = completable(p, d, e.getKey());
            for (int i = 0; i < q.objectives.size(); i++) {
                QuestDef.Objective o = q.objectives.get(i);
                if (!"kill".equals(o.type) || prog[i] >= o.count) continue;
                boolean match = o.target.equals("*")
                        || (o.target.equals("boss") && boss)
                        || o.target.equalsIgnoreCase(mobKey)
                        || o.target.equalsIgnoreCase(typeId);
                if (!match) continue;
                prog[i]++;
                Component what = o.target.equals("*") ? Component.literal("Monstres vaincus")
                        : o.target.equals("boss") ? Component.literal("Boss vaincus")
                        : o.target.contains(":") ? Component.translatable("entity." + o.target.replace(':', '.'))
                        : Component.translatable("entity.mmorpg." + o.target);
                p.sendOverlayMessage(Component.literal("✦ " + q.name + " — ").withColor(0xFFD040)
                        .append(what.copy().withColor(0xFFFFFF))
                        .append(Component.literal(" : " + prog[i] + "/" + o.count).withColor(0xFFFFFF)));
                d.dirty = true;
            }
            if (!wasComplete && completable(p, d, e.getKey())) {
                Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.QUEST, "OBJECTIFS ACCOMPLIS",
                        q.name + " — retournez voir un Maître des Quêtes", 0x60FF80));
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);
            }
        }
    }
}
