package com.mmorpg.dungeon;

import com.mmorpg.entity.NpcEntity;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Maitre des donjons : le PNJ place par {@code /setdonjon}. Il propose tous les donjons (ou un seul, selon son groupe « donjon:<id> »),
 * avec trois difficultes ; le joueur choisit, un ecran de chargement s'affiche et il entre dans son propre exemplaire.
 */
public final class DungeonMaster {
    public static final String GROUP_ALL = "donjon:*";

    private DungeonMaster() {
    }

    /** Niveau requis pour une difficulte (+5 par palier). */
    public static int requiredLevel(DungeonType t, int difficulty) {
        return t.level + 5 * difficulty;
    }

    private static boolean offers(NpcEntity npc, DungeonType t) {
        if(npc==null)return true;
        String g = npc.group();
        return g.isEmpty() || g.equals(GROUP_ALL) || g.equals("donjon:" + t.id);
    }

    public static void open(ServerPlayer p, NpcEntity npc) {
        if (!com.mmorpg.server.PortalAccess.require(p,"donjon")) return;
        var server = p.level().getServer();
        int level = RpgPlayers.get(p).level;
        String owner = InstanceManager.ownerKey(p);
        ListTag list = new ListTag();
        for (DungeonType t : DungeonType.values()) {
            if (!offers(npc, t)) continue;
            CompoundTag e = new CompoundTag();
            e.putString("id", t.id);
            e.putString("label", t.label);
            e.putInt("level", t.level);
            e.putString("lore", StyledDungeon.loreOf(t));
            e.putInt("color", StyledDungeon.colorOf(t));
            for (int d = 0; d < 3; d++) {
                e.putInt("need" + d, requiredLevel(t, d));
                e.putBoolean("resume" + d, InstanceManager.hasInstance(server, InstanceManager.pool(t, d), owner));
            }
            list.add(e);
        }
        CompoundTag data = new CompoundTag();
        data.putInt("npc", npc==null?-1:npc.getId());
        data.putString("name", npc==null?"Portail des donjons":npc.displayName());
        data.putInt("playerLevel", level);
        data.put("dungeons", list);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.DUNGEON_MASTER, data));
    }

    /** Choix du joueur : verifications cote serveur (distance, niveau, donjon propose) puis entree. */
    public static void enter(ServerPlayer p, Payloads.DungeonEnter packet) {
        Entity e = p.level().getEntity(packet.npc());
        NpcEntity npc=e instanceof NpcEntity n?n:null;
        if(packet.npc()==-1){if(!com.mmorpg.server.AdventurePortals.nearDungeonMenu(p))return;}
        else if(npc==null||npc.role()!=NpcEntity.Role.DONJON||npc.distanceToSqr(p)>100||!p.hasLineOfSight(npc))return;
        DungeonType t = DungeonType.byId(packet.dungeon());
        int d = packet.difficulty();
        if (t == null || d < 0 || d > 2 || !offers(npc, t)) return;
        if (RpgPlayers.get(p).level < requiredLevel(t, d) && !p.hasInfiniteMaterials()) {
            p.sendOverlayMessage(Component.literal("Niveau " + requiredLevel(t, d) + " requis pour ce donjon.").withColor(0xFF5050));
            return;
        }
        InstanceManager.enterPool(p, InstanceManager.pool(t, d));
    }
}
