package com.mmorpg.server;

import com.mmorpg.character.CharKey;
import com.mmorpg.character.CharacterStore;
import com.mmorpg.character.Characters;
import com.mmorpg.guild.GuildService;
import com.mmorpg.guild.GuildStore;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.store.StoreLedger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Controles serveur de la selection de personnage (donnees separees par personnage, guilde, emplacements, suppression). */
public final class DevCharacterChecks {
    private static void check(boolean b, String text) { if (!b) throw new AssertionError("characters: " + text); }

    private static CompoundTag packet(String action, int slot, String... kv) {
        var t = new CompoundTag();
        t.putString("action", action);
        t.putInt("slot", slot);
        for (int i = 0; i < kv.length; i += 2) t.putString(kv[i], kv[i + 1]);
        return t;
    }

    public static void run(ServerPlayer p) throws Exception {
        var server = p.level().getServer();
        check("lobby".equals(com.mmorpg.network.NetworkConfig.serverFor(com.mmorpg.network.NetworkConfig.Role.LOBBY)), "role LOBBY et nom de serveur du proxy");
        var d = RpgPlayers.get(p);
        UUID account = p.getUUID();
        check(d.charSlot == 0 && CharKey.of(p).equals(account), "personnage 0 = cle du compte (migration sans perte)");
        d.playerClass = PlayerClass.GUERRIER; d.level = 12; d.gold = 1234;
        p.getInventory().clearContent();
        p.getInventory().add(new ItemStack(ModItems.PIECE_OR.get(), 7));
        var bank = com.mmorpg.economy.BankData.get(server).account(account); bank.gold = 55;
        // connexion : l'ecran bloque l'initialisation ; le personnage actuel est celui de l'emplacement 0
        check(Characters.intercept(p) && Characters.selecting(p), "connexion : ecran de choix ouvert");
        // un emplacement non achete est refuse
        Characters.action(p, packet("create", 3, "name", "Trop Loin"));
        check(Characters.selecting(p) && RpgPlayers.get(p).charSlot == 0, "emplacement non achete refuse");
        Characters.action(p, packet("create", 1, "name", "x"));
        check(Characters.selecting(p), "nom trop court refuse");
        // creation d'un second personnage : etat vierge, ancien personnage range
        Characters.action(p, packet("create", 1, "name", "Elara"));
        check(!Characters.selecting(p), "creation : le jeu reprend");
        var n = RpgPlayers.get(p);
        check(n.charSlot == 1 && "Elara".equals(n.charName) && n.level == 1 && n.gold == 0 && !n.playerClass.isPlayable(), "nouveau personnage vierge");
        check(p.getInventory().isEmpty() && com.mmorpg.economy.BankData.get(server).account(account).gold == 0, "inventaire et banque vierges");
        check(p.getDisplayName().getString().contains("Elara") && !p.getDisplayName().getString().contains(p.getGameProfile().name()), "chat : nom du personnage actif");
        check(p.getTabListDisplayName() != null && p.getTabListDisplayName().getString().contains("Elara") && !p.getTabListDisplayName().getString().contains(p.getGameProfile().name()), "tab : nom du personnage actif");
        var store = CharacterStore.load(server, account);
        check(store.slots.get(0).snapshot != null && store.slots.get(0).level == 12 && store.slots.get(1).name.equals("Elara"), "fiche : personnage 0 range, personnage 1 resume");
        // guildes separees : le personnage 1 fonde sa guilde, le 0 n'en a pas
        n.gold = 100;
        GuildService.action(p, packet("create", 0, "name", "Cendres de Test"));
        var g1 = GuildStore.access(server, false, s -> GuildStore.of(s, CharKey.of(p)));
        check(g1 != null && g1.members.containsKey(CharKey.key(account, 1)) && !g1.members.containsKey(account), "guilde du personnage 1");
        // retour au personnage 0
        check(Characters.intercept(p), "reconnexion");
        Characters.action(p, packet("create", 1, "name", "Autre"));
        check(Characters.selecting(p), "emplacement occupe refuse");
        Characters.action(p, packet("play", 0));
        var z = RpgPlayers.get(p);
        check(z.charSlot == 0 && z.level == 12 && z.gold == 1234 && z.playerClass == PlayerClass.GUERRIER, "personnage 0 restaure (RPG)");
        check(p.getInventory().countItem(ModItems.PIECE_OR.get()) == 7 && com.mmorpg.economy.BankData.get(server).account(account).gold == 55, "personnage 0 restaure (inventaire, banque)");
        check(GuildStore.access(server, false, s -> GuildStore.of(s, CharKey.of(p))) == null, "le personnage 0 n'a pas la guilde du 1");
        // suppression : mauvais nom refuse, puis personnage 1 (chef seul) efface avec sa guilde
        check(Characters.intercept(p), "reconnexion 2");
        Characters.action(p, packet("delete", 1, "confirm", "Mauvais"));
        check(CharacterStore.load(server, account).slots.containsKey(1), "suppression sans bon nom refusee");
        Characters.action(p, packet("delete", 1, "confirm", "elara"));
        var pend = CharacterStore.load(server, account).slots.get(1);
        check(pend != null && pend.deleteAt > System.currentTimeMillis() + 23L * 3600_000, "suppression differee de 24 h");
        Characters.action(p, packet("play", 1));
        check(Characters.selecting(p), "jeu refuse pendant la suppression");
        Characters.action(p, packet("undelete", 1));
        check(CharacterStore.load(server, account).slots.get(1).deleteAt == 0, "suppression annulee");
        Characters.action(p, packet("delete", 1, "confirm", "elara"));
        var exp = CharacterStore.load(server, account);
        exp.slots.get(1).deleteAt = 1;                                  // delai ecoule
        exp.write();
        Characters.action(p, packet("open", 0));
        check(!CharacterStore.load(server, account).slots.containsKey(1) && GuildStore.access(server, false, s -> s.guilds.stream().noneMatch(x -> x.name.equals("Cendres de Test"))), "personnage 1 efface avec sa guilde vide");
        check(Characters.selecting(p) && RpgPlayers.get(p).charSlot == 0, "personnage actif intact apres suppression d'un autre");
        // achat d'emplacements avec credits
        check(Characters.ownedSlots(server, account) == 3, "3 emplacements gratuits");
        Characters.action(p, packet("buy", 0));
        check(Characters.ownedSlots(server, account) == 3, "achat refuse sans credits");
        StoreLedger.credit(server, account, StoreLedger.slotPrice(server) * 9, "test-slots-" + account);
        for (int i = 0; i < 12; i++) Characters.action(p, packet("buy", 0));
        check(Characters.ownedSlots(server, account) == 12, "plafond a 12 emplacements");
        // fin : on joue le personnage 0
        Characters.action(p, packet("play", 0));
        check(!Characters.selecting(p), "C'est parti");
        com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] CHARACTERS PASS: 3 emplacements, 12 max, donnees/banque/guilde separees, migration slot 0, suppression");
    }
}
