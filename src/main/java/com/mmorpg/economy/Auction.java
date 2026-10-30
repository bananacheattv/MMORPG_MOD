package com.mmorpg.economy;

import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.server.CrateOnly;
import com.mmorpg.server.RpgPlayers;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hotel des ventes : on y met en vente l'objet qu'on tient (prix total fixe), les autres joueurs l'achetent ; le vendeur recupere l'or
 * (moins 5 % de taxe) au comptoir. Une annonce dure 48 heures puis l'objet est rendu au comptoir. Tout est verifie cote serveur.
 */
public final class Auction {
    private Auction() {
    }

    public static final int PAGE_SIZE = 10;
    public static final int MAX_LISTINGS = 12;
    public static final long DURATION_MS = 48L * 3600 * 1000;
    public static final long MAX_PRICE = 100_000_000L;
    public static final double TAX = 0.05;

    public static final int ACT_BROWSE = 0, ACT_BUY = 1, ACT_SELL = 2, ACT_CANCEL = 3, ACT_CLAIM = 4;

    private static boolean near(ServerPlayer p, BlockPos pos) {
        return p.level().getBlockState(pos).is(ModBlocks.HDV.get()) && p.distanceToSqr(Vec3.atCenterOf(pos)) < 100;
    }

    private static Tag encode(MinecraftServer server, ItemStack stack) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
        DataResult<Tag> r = ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack);
        return r.result().orElse(new CompoundTag());
    }

    /** Retire les annonces expirees : leur objet attend le vendeur au comptoir. */
    private static void expire(AuctionData data) {
        long now = System.currentTimeMillis();
        for (AuctionData.Listing l : new ArrayList<>(data.listings())) {
            if (l.expires() <= now) {
                data.addReturn(l.seller(), l.stack());
                data.remove(l);
            }
        }
    }

    /** Envoie la page demandee des annonces (filtrees par nom), les annonces du joueur et ce qui l'attend au comptoir. */
    public static void open(ServerPlayer p, BlockPos pos, String query, int page) {
        MinecraftServer server = p.level().getServer();
        AuctionData data = AuctionData.get(server);
        expire(data);
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).strip();
        List<AuctionData.Listing> found = new ArrayList<>();
        for (AuctionData.Listing l : data.listings()) {
            if (!q.isEmpty() && !l.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) continue;
            found.add(l);
        }
        found.sort((a, b) -> Long.compare(b.id(), a.id()));
        int pages = Math.max(1, (found.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        CompoundTag t = new CompoundTag();
        t.putLong("pos", pos.asLong());
        t.putString("query", query == null ? "" : query);
        t.putInt("page", page);
        t.putInt("pages", pages);
        t.putInt("total", found.size());
        ListTag shown = new ListTag();
        for (int i = page * PAGE_SIZE; i < Math.min(found.size(), (page + 1) * PAGE_SIZE); i++) shown.add(entry(server, found.get(i), p));
        t.put("listings", shown);
        ListTag mine = new ListTag();
        for (AuctionData.Listing l : data.listings()) if (l.seller().equals(p.getUUID())) mine.add(entry(server, l, p));
        t.put("mine", mine);
        t.putLong("earnings", data.earnings(p.getUUID()));
        t.putInt("returns", data.returnCount(p.getUUID()));
        RpgPlayers.depositCoins(p);
        t.putLong("wallet", RpgPlayers.get(p).gold);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.AUCTION, t));
    }

    private static CompoundTag entry(MinecraftServer server, AuctionData.Listing l, ServerPlayer viewer) {
        CompoundTag e = new CompoundTag();
        e.putLong("id", l.id());
        e.putString("seller", l.sellerName());
        e.putLong("price", l.price());
        e.putLong("left", Math.max(0, l.expires() - System.currentTimeMillis()));
        e.put("stack", encode(server, l.stack()));
        return e;
    }

    public static void action(ServerPlayer p, BlockPos pos, int action, long a, String s, int page) {
        if(!com.mmorpg.server.FeatureFlags.allow(p,"hdv"))return;
        if (!near(p, pos) || com.mmorpg.hub.ClassTrial.refuseTransfer(p, null)) return;
        MinecraftServer server = p.level().getServer();
        AuctionData data = AuctionData.get(server);
        expire(data);
        String query = s == null ? "" : s;
        switch (action) {
            case ACT_BROWSE -> {
            }
            case ACT_BUY -> buy(p, data, a);
            case ACT_SELL -> {
                int slot=p.getInventory().getSelectedSlot();String fingerprint="";
                if(query.startsWith("slot:")){String[] choice=query.split(":",3);try{slot=Integer.parseInt(choice[1]);fingerprint=choice.length>2?choice[2]:"";}catch(RuntimeException e){return;}}
                sell(p,data,a,slot,fingerprint);query="";
            }
            case ACT_CANCEL -> cancel(p, data, a);
            case ACT_CLAIM -> claim(p, data);
            default -> {
            }
        }
        open(p, pos, query, page);
    }

    private static void buy(ServerPlayer p, AuctionData data, long id) {
        AuctionData.Listing l = data.find(id);
        if (l == null) {
            p.sendOverlayMessage(Component.literal("Cette annonce n'existe plus.").withColor(0xFF7060));
            return;
        }
        if (l.seller().equals(p.getUUID())) {
            p.sendOverlayMessage(Component.literal("C'est votre propre annonce : annulez-la pour récupérer l'objet.").withColor(0xFF7060));
            return;
        }
        RpgPlayers.depositCoins(p);
        if (!RpgPlayers.takeGold(p, l.price())) {
            p.sendOverlayMessage(Component.literal("Or insuffisant (" + l.price() + " requis).").withColor(0xFF7060));
            return;
        }
        data.remove(l);
        long payout = Math.max(1, l.price() - Math.round(l.price() * TAX));
        data.addEarnings(l.seller(), payout);
        ItemStack bought = l.stack().copy();
        com.mmorpg.db.Audit.log(p, com.mmorpg.db.Audit.AUCTION_BUY, l.price(), bought.getHoverName().getString() + " x" + bought.getCount() + " (vendeur " + l.seller() + ")");
        com.mmorpg.db.Audit.log(l.seller(), null, com.mmorpg.db.Audit.AUCTION_SALE, payout, bought.getHoverName().getString() + " x" + bought.getCount() + " (acheteur " + p.getGameProfile().name() + ", prix " + l.price() + ")");
        if (!p.getInventory().add(bought)) p.drop(bought, false, net.minecraft.util.Prediction.SERVER_ONLY);
        p.level().playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.COINS.get(), SoundSource.NEUTRAL, 0.9f, 1f);
        p.sendOverlayMessage(Component.literal("Achat : " + bought.getHoverName().getString() + " x" + bought.getCount() + " (-" + l.price() + " or)").withColor(0xFFD040));
        ServerPlayer seller = p.level().getServer().getPlayerList().getPlayer(l.seller());
        if (seller != null) {
            seller.sendSystemMessage(Component.literal("✦ Hôtel des ventes : votre « " + bought.getHoverName().getString() + " » a été vendu pour " + l.price()
                    + " or. Récupérez les " + payout + " or au comptoir.").withColor(0xFFD040));
        }
    }

    public static String saleFingerprint(ItemStack stack,net.minecraft.core.RegistryAccess registries){
        try{var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,registries);var tag=ItemStack.OPTIONAL_CODEC.encodeStart(ops,stack).getOrThrow();return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(tag.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void sell(ServerPlayer p, AuctionData data, long price,int slot,String fingerprint) {
        var bags=com.mmorpg.server.Backpacks.inventory(p);if(slot<0||slot>=36+com.mmorpg.server.Backpacks.capacity(bags))return;
        ItemStack held = slot<36?p.getInventory().getItem(slot):bags.getItem(slot-34);
        if(!fingerprint.isEmpty()&&!fingerprint.equals(saleFingerprint(held,p.level().registryAccess())))return;
        if (held.isEmpty()) {
            p.sendOverlayMessage(Component.literal("Tenez en main l'objet à mettre en vente.").withColor(0xFF7060));
            return;
        }
        if (price < 1 || price > MAX_PRICE) {
            p.sendOverlayMessage(Component.literal("Prix invalide (1 à " + MAX_PRICE + " or).").withColor(0xFF7060));
            return;
        }
        if (CrateOnly.is(held.getItem())) {
            p.sendOverlayMessage(Component.literal("Cet objet ne peut pas être vendu aux enchères.").withColor(0xFF7060));
            return;
        }
        int mine = 0;
        for (AuctionData.Listing l : data.listings()) if (l.seller().equals(p.getUUID())) mine++;
        if (mine >= com.mmorpg.server.RpgPlayers.get(p).auctionSlots) {
            p.sendOverlayMessage(Component.literal("Vous avez déjà " + com.mmorpg.server.RpgPlayers.get(p).auctionSlots + " annonces en cours.").withColor(0xFF7060));
            return;
        }
        ItemStack sold = held.copy();
        if(slot<36)p.getInventory().setItem(slot,ItemStack.EMPTY);else bags.setItem(slot-34,ItemStack.EMPTY);
        data.add(new AuctionData.Listing(data.nextId(), p.getUUID(), p.getGameProfile().name(), sold, price, System.currentTimeMillis() + DURATION_MS));
        com.mmorpg.db.Audit.log(p, com.mmorpg.db.Audit.AUCTION_LIST, price, sold.getHoverName().getString() + " x" + sold.getCount());
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.8f, 1.1f);
        p.sendOverlayMessage(Component.literal("Mis en vente : " + sold.getHoverName().getString() + " x" + sold.getCount() + " pour " + price + " or (48 h).").withColor(0xFFD040));
    }

    private static void cancel(ServerPlayer p, AuctionData data, long id) {
        AuctionData.Listing l = data.find(id);
        if (l == null || !l.seller().equals(p.getUUID())) return;
        data.remove(l);
        ItemStack back = l.stack().copy();
        if (!p.getInventory().add(back)) data.addReturn(p.getUUID(), back);
        p.sendOverlayMessage(Component.literal("Annonce annulée : l'objet vous est rendu.").withColor(0xFFD040));
    }

    private static void claim(ServerPlayer p, AuctionData data) {
        long gold = data.takeEarnings(p.getUUID());
        List<ItemStack> back = data.takeReturns(p.getUUID());
        if (gold <= 0 && back.isEmpty()) {
            p.sendOverlayMessage(Component.literal("Rien à récupérer au comptoir.").withColor(0xFF7060));
            return;
        }
        if (gold > 0) RpgPlayers.addGold(p, gold, false);
        for (ItemStack st : back) {
            if (!p.getInventory().add(st)) p.drop(st, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        p.level().playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.COINS.get(), SoundSource.NEUTRAL, 0.9f, 1.2f);
        p.sendOverlayMessage(Component.literal("Comptoir : " + gold + " or" + (back.isEmpty() ? "" : " et " + back.size() + " objet(s) récupérés.")).withColor(0xFFD040));
    }
}
