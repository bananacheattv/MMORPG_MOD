package com.mmorpg.server;

import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.world.AdventurePortalData.Portal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Outil portail (operateurs) : on choisit la destination dans un ecran, puis un clic droit sur un bloc en fait un portail. */
public final class PortalTool {
    private PortalTool() {}

    private record Config(String kind, int region, String dungeon, int difficulty) {}
    private static final Map<UUID, Config> CONFIG = new ConcurrentHashMap<>();

    public static boolean allowed(ServerPlayer p) {
        if (p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        p.sendOverlayMessage(Component.literal("L'outil portail est réservé aux opérateurs.").withColor(0xFF8060));
        return false;
    }

    public static void open(ServerPlayer p) {
        if (!allowed(p)) return;
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.PORTAL_TOOL, new CompoundTag()));
    }

    public static void action(ServerPlayer p, CompoundTag t) {
        if (!allowed(p)) return;
        var c = new Config(t.getStringOr("kind", "hub"), t.getIntOr("region", -1), t.getStringOr("dungeon", ""), t.getIntOr("difficulty", 0));
        var probe = new Portal(UUID.randomUUID(), p.level().dimension(), BlockPos.ZERO, true, c.kind, c.region, c.dungeon, c.difficulty, true);
        boolean menu = c.kind.equals("donjons") || ((c.kind.equals("mine") || c.kind.equals("plaines")) && c.region < 0);
        if (menu) { p.sendSystemMessage(Component.literal("Niveau obligatoire : choisissez une région ou un donjon précis.").withColor(0xFF8080)); return; }
        if (!AdventurePortals.valid(probe)) { p.sendSystemMessage(Component.literal("Destination invalide.").withColor(0xFF8080)); return; }
        CONFIG.put(p.getUUID(), c);
        p.sendSystemMessage(Component.literal("Destination : " + AdventurePortals.label(probe) + ". Clic droit sur un bloc pour en faire un portail.").withColor(0x80E0A0));
    }

    /** Clic droit sur un bloc avec l'outil. */
    public static void use(ServerPlayer p, BlockPos pos) {
        if (!allowed(p)) return;
        var c = CONFIG.get(p.getUUID());
        if (c == null) { open(p); return; }
        Portal bp = new Portal(UUID.randomUUID(), p.level().dimension(), pos, true, c.kind, c.region, c.dungeon, c.difficulty, true);
        if (!AdventurePortals.place(p, bp)) return;
        p.sendSystemMessage(Component.literal("Bloc portail : " + AdventurePortals.label(bp) + ". Marchez dessus pour entrer. /portail supprimer : retirer, /undoportail : annuler.").withColor(0x80E0A0));
    }
}
