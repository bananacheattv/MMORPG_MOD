package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.Multiblocks;
import com.mmorpg.economy.Auction;
import com.mmorpg.economy.Mail;
import com.mmorpg.economy.MailData;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.UUID;

/** Verifications serveur du courrier : ./gradlew runServer -PmailChecks (monde run/ isole uniquement). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevMailChecks {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError("Mail check failed: " + label);
    }

    private static ServerPlayer fake(net.minecraft.server.level.ServerLevel level, String name, BlockPos at) {
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
        p.setData(com.mmorpg.registry.ModAttachments.PLAYER, new PlayerData());
        RpgPlayers.get(p).playerClass = PlayerClass.GUERRIER;
        RpgPlayers.get(p).level = 50;
        p.getInventory().clearContent();
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() - 3.5, 0, 0);
        return p;
    }

    private static String pick(ServerPlayer p, int slot) {
        return slot + ":" + Auction.saleFingerprint(p.getInventory().getItem(slot), p.level().registryAccess());
    }

    private static String text(String to, String msg, String picks) {
        return to + Mail.SEP + msg + Mail.SEP + picks;
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkMail")) return;
        var server = event.getServer();
        var level = server.overworld();
        BlockPos box = new BlockPos(0, 150, 0), hdv = new BlockPos(30, 150, 0);
        try {
            for (int x = -5; x <= 40; x++) for (int y = 0; y <= 8; y++) for (int z = -6; z <= 6; z++) level.setBlock(new BlockPos(x, 150 + y, z), Blocks.AIR.defaultBlockState(), 3);
            var kind = MultiblockKind.BOITE_AUX_LETTRES;
            check(Multiblocks.hasRoom(level, box, kind, Direction.NORTH), "room for the mailbox");
            level.setBlock(box, ModBlocks.BOITE_AUX_LETTRES.get().defaultBlockState(), 3);
            Multiblocks.placeParts(level, box, kind, Direction.NORTH);
            check(!level.getBlockState(box.above()).isAir() && !level.getBlockState(box.above(2)).isAir(), "invisible parts placed");
            check(!kind.masterShape(Direction.NORTH).isEmpty(), "master collision");
            for (String res : new String[]{"textures/entity/structure/boite_aux_lettres.png", "textures/entity/structure/boite_aux_lettres_emissive.png",
                    "blockstates/boite_aux_lettres.json", "models/block/boite_aux_lettres.json", "items/boite_aux_lettres.json"}) {
                check(DevMailChecks.class.getResource("/assets/mmorpg/" + res) != null, "resource " + res);
            }
            check(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(MMORPG.id("boite_aux_lettres")), "block item registered");

            var a = fake(level, "MailA", box);
            var b = fake(level, "MailB", box);
            MailData data = MailData.get(server);
            Mail.remember(a);
            Mail.remember(b);
            RpgPlayers.get(a).gold = 500;
            a.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
            a.getInventory().setItem(1, new ItemStack(Items.IRON_INGOT, 2));

            // refus : soi-meme, inconnu, trop d'objets, mauvaise empreinte, trop de gold, trop loin, lettre vide
            Mail.action(a, box, Mail.ACT_SEND, 10, text("MailA", "x", ""));
            Mail.action(a, box, Mail.ACT_SEND, 10, text("Inconnu_zzz", "x", ""));
            Mail.action(a, box, Mail.ACT_SEND, 10, text("MailB", "x", "0:deadbeef"));
            Mail.action(a, box, Mail.ACT_SEND, 9_999, text("MailB", "x", ""));
            Mail.action(a, box, Mail.ACT_SEND, 0, text("MailB", "", ""));
            Mail.action(a, box, Mail.ACT_SEND, 0, text("MailB", "x", "0:0,0:0"));
            a.snapTo(box.getX() + 0.5, box.getY(), box.getZ() + 40, 0, 0);
            Mail.action(a, box, Mail.ACT_SEND, 10, text("MailB", "x", pick(a, 0)));
            a.snapTo(box.getX() + 0.5, box.getY(), box.getZ() - 3.5, 0, 0);
            check(data.count(b.getUUID()) == 0 && data.count(a.getUUID()) == 0, "refused letters not delivered");
            check(RpgPlayers.get(a).gold == 500 && a.getInventory().getItem(0).getCount() == 3, "nothing lost on refusal");

            // envoi : or et deux objets
            Mail.action(a, box, Mail.ACT_SEND, 200, text("MailB", "Bonjour", pick(a, 0) + "," + pick(a, 1)));
            check(RpgPlayers.get(a).gold == 300, "gold taken from sender");
            check(a.getInventory().getItem(0).isEmpty() && a.getInventory().getItem(1).isEmpty(), "items taken from sender");
            check(data.count(b.getUUID()) == 1, "letter delivered (offline recipient)");
            var letter = data.box(b.getUUID()).get(0);
            check(letter.gold() == 200 && letter.items().size() == 2 && letter.message().equals("Bonjour") && !letter.system(), "letter content");

            // la lettre survit a l'enregistrement
            var ops = server.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            var enc = MailData.Letter.CODEC.encodeStart(ops, letter).getOrThrow();
            var back = MailData.Letter.CODEC.parse(ops, enc).getOrThrow();
            check(back.gold() == 200 && back.items().size() == 2 && back.items().get(0).is(Items.DIAMOND), "letter codec round trip");

            // relevee, inventaire plein : l'or est pris une fois, les objets restants attendent
            RpgPlayers.get(b).gold = 0;
            for (int i = 0; i < 36; i++) b.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            Mail.action(b, box, Mail.ACT_CLAIM, letter.id(), "");
            check(RpgPlayers.get(b).gold == 200, "gold claimed");
            check(data.count(b.getUUID()) == 1 && data.box(b.getUUID()).get(0).gold() == 0 && data.box(b.getUUID()).get(0).items().size() == 2, "items kept when inventory full");
            Mail.action(b, box, Mail.ACT_CLAIM, letter.id(), "");
            check(RpgPlayers.get(b).gold == 200, "gold not claimed twice");
            b.getInventory().clearContent();
            Mail.action(b, box, Mail.ACT_CLAIM_ALL, 0, "");
            check(data.count(b.getUUID()) == 0, "letter removed once claimed");
            check(b.getInventory().countItem(Items.DIAMOND) == 3 && b.getInventory().countItem(Items.IRON_INGOT) == 2, "items received");
            Mail.action(b, box, Mail.ACT_CLAIM, 99999, "");
            check(RpgPlayers.get(b).gold == 200, "unknown letter ignored");

            // un autre joueur ne peut pas relever la lettre d'autrui
            Mail.deliver(server, b.getUUID(), a.getUUID(), "MailA", "secret", 50, java.util.List.of(), false);
            long id = data.box(b.getUUID()).get(0).id();
            Mail.action(a, box, Mail.ACT_CLAIM, id, "");
            check(data.count(b.getUUID()) == 1 && RpgPlayers.get(a).gold == 300, "cannot claim someone else's letter");
            Mail.action(b, box, Mail.ACT_CLAIM, id, "");

            // boite pleine
            for (int i = 0; i < Mail.MAX_LETTERS; i++) check(Mail.deliver(server, a.getUUID(), null, "Test", "", 0, java.util.List.of(), true), "fill box " + i);
            check(!Mail.deliver(server, a.getUUID(), null, "Test", "", 0, java.util.List.of(), true), "full box refuses");
            for (var l : new java.util.ArrayList<>(data.box(a.getUUID()))) data.remove(a.getUUID(), l);

            // achat a l'HDV : l'objet arrive dans la boite aux lettres
            level.setBlock(hdv, ModBlocks.HDV.get().defaultBlockState(), 3);
            Multiblocks.placeParts(level, hdv, MultiblockKind.HDV, Direction.NORTH);
            a.snapTo(hdv.getX() + 0.5, hdv.getY(), hdv.getZ() - 3.5, 0, 0);
            b.snapTo(hdv.getX() + 0.5, hdv.getY(), hdv.getZ() - 3.5, 0, 0);
            a.getInventory().setItem(2, new ItemStack(Items.EMERALD, 5));
            Auction.action(a, hdv, Auction.ACT_SELL, 40, "slot:2:" + Auction.saleFingerprint(a.getInventory().getItem(2), level.registryAccess()), 0);
            var listings = com.mmorpg.economy.AuctionData.get(server).listings();
            check(listings.size() >= 1, "listing created");
            long lid = listings.get(listings.size() - 1).id();
            RpgPlayers.get(b).gold = 100;
            Auction.action(b, hdv, Auction.ACT_BUY, lid, "", 0);
            check(RpgPlayers.get(b).gold == 60, "buyer paid");
            check(b.getInventory().countItem(Items.EMERALD) == 0, "bought item not in inventory");
            check(data.count(b.getUUID()) == 1 && data.box(b.getUUID()).get(0).system() && data.box(b.getUUID()).get(0).items().get(0).is(Items.EMERALD), "purchase delivered to the mailbox");
            Mail.action(b, box, Mail.ACT_CLAIM_ALL, 0, "");
            b.snapTo(box.getX() + 0.5, box.getY(), box.getZ() - 3.5, 0, 0);
            Mail.action(b, box, Mail.ACT_CLAIM_ALL, 0, "");
            check(b.getInventory().countItem(Items.EMERALD) == 5 && data.count(b.getUUID()) == 0, "purchase claimed at the mailbox");
            MMORPG.LOGGER.info("[MMORPG] PASS mail checks");
        } catch (Throwable t) {
            MMORPG.LOGGER.error("[MMORPG] FAIL mail checks", t);
        }
        server.halt(false);
    }
}
