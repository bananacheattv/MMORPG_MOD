package com.mmorpg.dungeon;

import com.mmorpg.config.LootTables;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.server.LootEditor;
import com.mmorpg.server.QuestEditor;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Donjon de setup : un exemplaire reserve aux administrateurs de chaque donjon (genere comme un exemplaire normal). On y modifie ce
 * qu'on veut (blocs, generateurs de monstres, coffres) puis le menu « Enregistrer » en fait le modele copie dans tous les exemplaires
 * construits ensuite. Les coffres tirent leur contenu au hasard dans deux tables de butin editables depuis ce menu.
 */
public final class DungeonSetup {
    private DungeonSetup() {
    }

    /** Envoie l'administrateur dans le donjon de setup (construit au premier passage). */
    public static boolean enter(ServerPlayer p, DungeonType t) {
        if (!QuestEditor.allowed(p)) return false;
        boolean ok = InstanceManager.enterLocal(p, InstanceManager.pool(t, InstanceManager.SETUP), false);
        if (ok) p.sendSystemMessage(Component.literal("Donjon de setup « " + t.label + " » : modifiez-le puis /mmorpg donjonsetup menu pour enregistrer.").withColor(0xFFE080));
        else p.sendSystemMessage(Component.literal("Setup indisponible ici (serveur de donjons requis).").withColor(0xFF6050));
        return ok;
    }

    private static InstanceData.Slot slotOf(ServerPlayer p) {
        var server = p.level().getServer();
        var slot = InstanceManager.slotAt(server, p.level(), p.getX(), p.getZ());
        return slot != null && InstanceManager.isSetupSlot(slot) ? slot : null;
    }

    public static void openMenu(ServerPlayer p) {
        if (!QuestEditor.allowed(p)) return;
        var slot = slotOf(p);
        if (slot == null) {
            p.sendSystemMessage(Component.literal("Vous n'êtes pas dans un donjon de setup (/mmorpg donjonsetup <donjon>).").withColor(0xFF6050));
            return;
        }
        send(p, InstanceManager.typeOf(slot.pool), "");
    }

    private static void send(ServerPlayer p, DungeonType t, String status) {
        CompoundTag d = new CompoundTag();
        d.putString("type", t.id);
        d.putString("label", t.label);
        d.putBoolean("template", DungeonTemplate.exists(t));
        d.putString("status", status);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.DUNGEON_SETUP, d));
    }

    public static void action(ServerPlayer p, CompoundTag data) {
        if (!QuestEditor.allowed(p)) return;
        var slot = slotOf(p);
        if (slot == null) return;
        DungeonType t = InstanceManager.typeOf(slot.pool);
        String act = data.getStringOr("do", "");
        switch (act) {
            case "loot" -> LootEditor.open(p, List.of(LootTables.dungeonRooms(t.id), LootTables.dungeonTreasure(t.id)));
            case "leave" -> {
                InstanceManager.returnToEntrance(p);
            }
            case "save" -> {
                String status;
                try {
                    int n = DungeonTemplate.capture((ServerLevel) p.level(), new net.minecraft.core.BlockPos(slot.x, slot.y - 1, slot.z), t);
                    int stale = markStale(p.level().getServer(), t);
                    status = "Modèle enregistré (" + n + " coffres/générateurs). Appliqué aux prochains exemplaires (" + stale + " à reconstruire).";
                } catch (Exception e) {
                    com.mmorpg.MMORPG.LOGGER.error("[MMORPG] Enregistrement du modele de donjon impossible", e);
                    status = "Enregistrement impossible : " + e.getMessage();
                }
                send(p, t, status);
            }
            case "delete" -> {
                boolean ok = DungeonTemplate.delete(t);
                if (ok) markStale(p.level().getServer(), t);
                send(p, t, ok ? "Modèle supprimé : les prochains exemplaires reviennent à la génération d'origine." : "Aucun modèle à supprimer.");
            }
            default -> send(p, t, "");
        }
    }

    /** Les exemplaires libres sont reconstruits tout de suite, les occupes a leur prochaine reservation. */
    private static int markStale(net.minecraft.server.MinecraftServer server, DungeonType t) {
        int n = 0;
        InstanceData data = InstanceData.get(server);
        for (var s : data.slots) {
            if (InstanceManager.isSetupSlot(s) || InstanceManager.typeOf(s.pool) != t) continue;
            n++;
            if (s.owner.isEmpty()) s.ready = false;
            else InstanceManager.STALE.add(s.pool + "#" + s.index);
        }
        data.dirty();
        return n;
    }
}
