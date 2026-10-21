package com.mmorpg.hub;

import com.mmorpg.entity.hub.ClassMasterEntity;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** Dialogues et actions des PNJ du hub : essayer une classe, la choisir, quitter l'essai, revoir la cinematique. */
public final class HubManager {
    /** Interrupteur reserve aux tests automatises (DevShowcase) : traite le joueur comme un non-operateur. */
    public static volatile boolean forceNonOperator = false;

    /** Operateur (permission de maitre du jeu) : exempte du choix de classe obligatoire et de l'essai prealable. */
    public static boolean isOperator(net.minecraft.server.level.ServerPlayer p) {
        return !forceNonOperator && p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    private HubManager() {
    }

    private static boolean isPlayable(int ordinal) {
        PlayerClass[] all = PlayerClass.values();
        return ordinal >= 0 && ordinal < all.length && all[ordinal].isPlayable();
    }

    /** Un maitre est-il a portee de dialogue ? */
    private static boolean nearMaster(ServerPlayer p) {
        return !p.level().getEntitiesOfClass(ClassMasterEntity.class, p.getBoundingBox().inflate(12, 6, 12)).isEmpty();
    }

    /** Ouvre le dialogue d'un maitre. */
    public static void interact(ServerPlayer p, ClassMasterEntity npc) {
        if (CinematicManager.isLocked(p)) return;
        HubPlayerData hd = ClassTrial.data(p);
        CompoundTag t = new CompoundTag();
        t.putInt("kind", npc.kind());
        t.putBoolean("inTrial", hd.inTrial);
        t.putInt("trialClass", hd.trialClass);
        t.putInt("trialLevel", hd.trialLevel);
        t.putBoolean("hasClass", RpgPlayers.get(p).playerClass.isPlayable() && !hd.inTrial);
        t.putInt("tried", hd.triedMask);
        t.putBoolean("operator", isOperator(p));
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.CLASS_MASTER, t));
    }

    /** Action recue du client (dialogue, cinematique). */
    public static void action(ServerPlayer p, int action, int a, int b) {
        switch (action) {
            case Payloads.HubAction.TRY -> {
                if (!nearMaster(p) || !isPlayable(a)) return;
                String err = ClassTrial.start(p, PlayerClass.values()[a], b);
                if (err != null) p.sendOverlayMessage(Component.literal(err).withColor(0xFF8080));
            }
            case Payloads.HubAction.CHOOSE -> choose(p, a);
            case Payloads.HubAction.QUIT -> {
                if (ClassTrial.active(p)) ClassTrial.end(p, true, "Essai terminé : votre personnage a été restauré.");
            }
            case Payloads.HubAction.REPLAY -> {
                if (nearMaster(p) && !CinematicManager.start(p, false)) p.sendOverlayMessage(Component.literal("Le hub n'est pas construit.").withColor(0xFF8080));
            }
            case Payloads.HubAction.CINEMATIC_END, Payloads.HubAction.CINEMATIC_SKIP -> CinematicManager.finish(p);
            default -> {
            }
        }
    }

    /** Choix definitif de la classe : seulement apres l'avoir essayee (les operateurs peuvent choisir directement). */
    private static void choose(ServerPlayer p, int ordinal) {
        if (!isPlayable(ordinal) || !nearMaster(p)) return;
        PlayerClass cls = PlayerClass.values()[ordinal];
        HubPlayerData hd = ClassTrial.data(p);
        boolean op = isOperator(p);
        if (!hd.tried(ordinal) && !op) {
            p.sendOverlayMessage(Component.literal("Essayez d'abord cette classe avant de la choisir.").withColor(0xFF8080));
            return;
        }
        if (ClassTrial.active(p)) ClassTrial.end(p, false, null);
        if (RpgPlayers.get(p).playerClass.isPlayable()) {
            p.sendOverlayMessage(Component.literal("Vous avez déjà une classe.").withColor(0xFF8080));
            return;
        }
        hd.cinematicSeen = true;
        HubData hub = HubData.get(p.level().getServer());
        ServerLevel level = hub.level(p.level().getServer());
        if (hub.exists() && level != null) {
            Vec3 at = HubLayout.center(hub.origin(), new int[]{HubLayout.CLASS_MASTER[0], 0, HubLayout.CLASS_MASTER[2] + 4});
            p.teleportTo(level, at.x, at.y, at.z, Set.of(), 0F, 0F, true);
        }
        RpgPlayers.selectClass(p, cls);
        ClassTrial.sendState(p);
    }
}
