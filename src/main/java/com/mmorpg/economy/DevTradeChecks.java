package com.mmorpg.economy;

import com.mmorpg.MMORPG;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

/** Verifie /trade sur de vrais joueurs simules : echange, annulation, falsification, inventaire plein, or insuffisant, conservation des objets. */
public final class DevTradeChecks {
    private DevTradeChecks() {
    }

    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError("Trade: " + why);
    }

    private static void act(ServerPlayer p, String act, int slot, long amount) {
        CompoundTag t = new CompoundTag();
        t.putString("act", act);
        t.putInt("slot", slot);
        t.putLong("amount", amount);
        TradeManager.action(p, t);
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) n += p.getInventory().getItem(i).getCount();
        return n;
    }

    private static void open(ServerPlayer a, ServerPlayer b) {
        TradeManager.request(a, b, b.getGameProfile().name());
        check(!TradeManager.active(a), "une demande n'ouvre rien avant l'acceptation");
        TradeManager.accept(b);
        check(TradeManager.active(a) && TradeManager.active(b), "l'acceptation ouvre l'echange des deux cotes");
    }

    private static void both(ServerPlayer a, ServerPlayer b, String act) {
        act(a, act, 0, 0);
        act(b, act, 0, 0);
    }

    public static void run(ServerPlayer host) {
        var level = host.level();
        ServerPlayer a = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "TradeA"));
        ServerPlayer b = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "TradeB"));
        TradeManager.clear();
        TradeManager.simulate(a);
        TradeManager.simulate(b);
        try {
            a.snapTo(host.getX(), host.getY(), host.getZ());
            b.snapTo(host.getX() + 2, host.getY(), host.getZ());
            a.getInventory().clearContent();
            b.getInventory().clearContent();
            RpgPlayers.get(a).gold = 0;
            RpgPlayers.get(b).gold = 0;
            RpgPlayers.addGold(a, 500, false);
            RpgPlayers.addGold(b, 100, false);

            // --- echange complet : objets et or dans les deux sens, apres validation et confirmation des deux
            a.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
            b.getInventory().setItem(1, new ItemStack(Items.EMERALD, 3));
            open(a, b);
            act(a, "offer", 0, 0);
            act(b, "offer", 1, 0);
            act(a, "gold", 0, 200);
            act(b, "gold", 0, 40);
            check(count(a, Items.DIAMOND) == 5 && count(b, Items.EMERALD) == 3, "les objets proposes restent dans l'inventaire avant l'echange");
            act(a, "ready", 0, 0);
            act(a, "confirm", 0, 0);
            check(TradeManager.active(a) && count(a, Items.DIAMOND) == 5, "confirmer sans validation des deux ne fait rien");
            act(b, "ready", 0, 0);
            both(a, b, "confirm");
            check(TradeManager.active(a) && count(a, Items.DIAMOND) == 5, "confirmation refusee avant la fin du delai de securite");
            TradeManager.skipConfirmDelay(a);
            act(a, "confirm", 0, 0);
            check(TradeManager.active(a) && count(a, Items.DIAMOND) == 5, "un seul joueur confirme : rien ne bouge");
            act(b, "confirm", 0, 0);
            check(!TradeManager.active(a) && !TradeManager.active(b), "echange termine des deux cotes");
            check(count(a, Items.DIAMOND) == 0 && count(b, Items.DIAMOND) == 5 && count(a, Items.EMERALD) == 3 && count(b, Items.EMERALD) == 0, "objets echanges sans perte ni copie");
            check(RpgPlayers.get(a).gold == 340 && RpgPlayers.get(b).gold == 260, "or echange (500-200+40 / 100-40+200)");

            // --- annulation : aucune perte, meme apres validation
            a.getInventory().setItem(2, new ItemStack(Items.NETHERITE_SWORD));
            open(a, b);
            act(a, "offer", 2, 0);
            both(a, b, "ready");
            act(b, "cancel", 0, 0);
            check(!TradeManager.active(a) && count(a, Items.NETHERITE_SWORD) == 1 && count(b, Items.NETHERITE_SWORD) == 0, "annulation sans perte");

            // --- modification apres validation : les validations sont annulees
            open(a, b);
            act(a, "offer", 2, 0);
            both(a, b, "ready");
            act(b, "offer", 3, 0);                                                          // case vide : sans effet
            act(b, "gold", 0, 10);
            TradeManager.skipConfirmDelay(a);
            act(a, "confirm", 0, 0);
            act(b, "confirm", 0, 0);
            check(TradeManager.active(a) && count(a, Items.NETHERITE_SWORD) == 1, "changer l'offre annule les validations : pas d'echange");
            check(RpgPlayers.get(b).gold == 260, "l'or n'est pas debite avant la fin");

            // --- falsification : l'objet propose est deplace avant la confirmation
            both(a, b, "ready");
            TradeManager.skipConfirmDelay(a);
            a.getInventory().setItem(7, a.getInventory().getItem(2));
            a.getInventory().setItem(2, ItemStack.EMPTY);
            both(a, b, "confirm");
            check(TradeManager.active(a) && count(a, Items.NETHERITE_SWORD) == 1 && count(b, Items.NETHERITE_SWORD) == 0, "objet deplace apres validation : echange refuse");
            check(RpgPlayers.get(b).gold == 260 && RpgPlayers.get(a).gold == 340, "aucun or ne bouge quand la verification echoue");
            act(a, "cancel", 0, 0);

            // --- falsification : objet remplace par un autre dans la meme case
            a.getInventory().setItem(2, new ItemStack(Items.NETHERITE_SWORD));
            a.getInventory().setItem(7, ItemStack.EMPTY);
            open(a, b);
            act(a, "offer", 2, 0);
            both(a, b, "ready");
            a.getInventory().setItem(2, new ItemStack(Items.STICK));
            TradeManager.skipConfirmDelay(a);
            both(a, b, "confirm");
            check(count(b, Items.STICK) == 0 && count(a, Items.STICK) == 1, "objet remplace dans la case : jamais transfere");
            act(b, "cancel", 0, 0);
            a.getInventory().setItem(2, new ItemStack(Items.NETHERITE_SWORD));

            // --- or insuffisant : le montant est plafonne au porte-monnaie
            open(a, b);
            act(a, "gold", 0, 999_999);
            check(TradeManager.offeredGold(a) == 340, "l'or propose est plafonne au porte-monnaie");
            act(b, "cancel", 0, 0);

            // --- inventaire plein : l'echange est refuse sans rien deplacer
            for (int i = 0; i < 36; i++) if (b.getInventory().getItem(i).isEmpty()) b.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            open(a, b);
            act(a, "offer", 2, 0);
            both(a, b, "ready");
            TradeManager.skipConfirmDelay(a);
            both(a, b, "confirm");
            check(TradeManager.active(a) && count(a, Items.NETHERITE_SWORD) == 1 && count(b, Items.NETHERITE_SWORD) == 0, "inventaire plein : rien ne bouge");
            act(a, "cancel", 0, 0);
            b.getInventory().clearContent();

            // --- demande : refus, expiration par distance, double demande
            TradeManager.request(a, b, "TradeB");
            TradeManager.decline(b);
            TradeManager.accept(b);
            check(!TradeManager.active(a), "demande refusee : impossible d'accepter ensuite");
            b.snapTo(host.getX() + 200, host.getY(), host.getZ());
            TradeManager.request(a, b, "TradeB");
            TradeManager.accept(b);
            check(!TradeManager.active(a), "trop loin : pas d'echange");
            b.snapTo(host.getX() + 2, host.getY(), host.getZ());

            // --- essai de classe : or et sac sont restaures a la fin de l'essai, aucun echange ni paiement possible
            com.mmorpg.hub.ClassTrial.data(a).inTrial = true;
            try {
                TradeManager.request(a, b, "TradeB");
                TradeManager.accept(b);
                check(!TradeManager.active(a), "essai de classe : demande d'echange refusee");
                TradeManager.request(b, a, "TradeA");
                TradeManager.accept(a);
                check(!TradeManager.active(a) && !TradeManager.active(b), "essai de classe : impossible d'accepter un echange");
                check(com.mmorpg.hub.ClassTrial.refuseTransfer(b, a) && com.mmorpg.hub.ClassTrial.refuseTransfer(a, null), "essai de classe : paiement refuse dans les deux sens");
            } finally {
                com.mmorpg.hub.ClassTrial.data(a).inTrial = false;
            }
            open(a, b);
            com.mmorpg.hub.ClassTrial.data(b).inTrial = true;
            try {
                act(a, "gold", 0, 10);
                check(!TradeManager.active(a) && !TradeManager.active(b), "joueur en essai pendant un echange : echange annule");
            } finally {
                com.mmorpg.hub.ClassTrial.data(b).inTrial = false;
            }

            // --- conservation : total d'objets et d'or identique a la fin de tous les scenarios
            check(count(a, Items.NETHERITE_SWORD) + count(b, Items.NETHERITE_SWORD) == 1, "une seule epee existe encore");
            check(RpgPlayers.get(a).gold + RpgPlayers.get(b).gold == 600, "l'or total est conserve");
            MMORPG.LOGGER.info("[TRADE CHECKS] TRADE_PASS: demande/acceptation, double confirmation, delai de securite, annulation sans perte, objet deplace ou remplace, inventaire plein, essai de classe, or conserve");
        } finally {
            TradeManager.clear();
        }
    }
}
