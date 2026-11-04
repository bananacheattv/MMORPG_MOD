package com.mmorpg.economy;

import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.server.CrateOnly;
import com.mmorpg.server.RpgPlayers;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Courrier d'Aeloria : on depose une lettre (message, or, jusqu'a 4 objets) a un joueur, en ligne ou non, dans une boite aux lettres ;
 * le destinataire la releve a n'importe quelle boite. Les achats de l'hotel des ventes y arrivent aussi. Tout est verifie cote serveur.
 */
public final class Mail {
    private Mail() {
    }

    public static final int MAX_LETTERS = 60, MAX_ITEMS = 4, MESSAGE_MAX = 140;
    public static final long MAX_GOLD = 100_000_000L;
    public static final int ACT_BROWSE = 0, ACT_SEND = 1, ACT_CLAIM = 2, ACT_CLAIM_ALL = 3;
    /** Separateur des champs du texte d'envoi : destinataire, message, objets joints (« case:empreinte,... »). */
    public static final char SEP = '\u0001';
    private static final UUID SYSTEM = new UUID(0, 0);

    private static boolean near(ServerPlayer p, BlockPos pos) {
        return p.level().getBlockState(pos).is(ModBlocks.BOITE_AUX_LETTRES.get()) && p.distanceToSqr(Vec3.atCenterOf(pos)) < 100;
    }

    private static Tag encode(MinecraftServer server, ItemStack stack) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
        return ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).result().orElse(new CompoundTag());
    }

    /** Retient le pseudonyme du joueur (annuaire des destinataires hors ligne). */
    public static void remember(ServerPlayer p) {
        MailData.get(p.level().getServer()).remember(p.getUUID(), p.getGameProfile().name());
    }

    /** Nombre de lettres en attente, envoye au client (drapeau des boites aux lettres). */
    public static void sendCount(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.putInt("count", MailData.get(p.level().getServer()).count(p.getUUID()));
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.MAIL_STATE, t));
    }

    /** A la connexion : annuaire, drapeau et rappel. */
    public static void onLogin(ServerPlayer p) {
        remember(p);
        int n = MailData.get(p.level().getServer()).count(p.getUUID());
        sendCount(p);
        if (n > 0) p.sendSystemMessage(Component.literal("✉ Vous avez " + n + " lettre(s) en attente dans les boîtes aux lettres.").withColor(0xFFD040));
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        MinecraftServer server = p.level().getServer();
        MailData data = MailData.get(server);
        data.remember(p.getUUID(), p.getGameProfile().name());
        CompoundTag t = new CompoundTag();
        t.putLong("pos", pos.asLong());
        ListTag letters = new ListTag();
        List<MailData.Letter> box = new ArrayList<>(data.box(p.getUUID()));
        box.sort((a, b) -> Long.compare(b.id(), a.id()));
        for (MailData.Letter l : box) {
            CompoundTag e = new CompoundTag();
            e.putLong("id", l.id());
            e.putString("from", l.fromName());
            e.putBoolean("system", l.system());
            e.putString("message", l.message());
            e.putLong("gold", l.gold());
            e.putLong("sent", l.sent());
            ListTag items = new ListTag();
            for (ItemStack s : l.items()) items.add(encode(server, s));
            e.put("items", items);
            letters.add(e);
        }
        t.put("letters", letters);
        RpgPlayers.depositCoins(p);
        t.putLong("wallet", RpgPlayers.get(p).gold);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.MAIL, t));
        sendCount(p);
    }

    /** Depose une lettre dans la boite d'un joueur (achat de l'HDV, par exemple) ; faux si sa boite est pleine. */
    public static boolean deliver(MinecraftServer server, UUID to, UUID from, String fromName, String message, long gold, List<ItemStack> items, boolean system) {
        MailData data = MailData.get(server);
        if (data.count(to) >= MAX_LETTERS) return false;
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack s : items) if (!s.isEmpty()) copy.add(s.copy());
        data.add(to, new MailData.Letter(data.nextId(), system ? SYSTEM : from, fromName, message, Math.max(0, gold), copy, System.currentTimeMillis(), system));
        ServerPlayer target = server.getPlayerList().getPlayer(to);
        if (target != null) {
            sendCount(target);
            target.sendSystemMessage(Component.literal("✉ Nouvelle lettre de " + fromName + " : relevez-la à une boîte aux lettres.").withColor(0xFFD040));
            target.level().playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 1.3f);
        }
        return true;
    }

    public static void action(ServerPlayer p, BlockPos pos, int action, long amount, String text) {
        if (!com.mmorpg.server.FeatureFlags.allow(p, "courrier")) return;
        if (!near(p, pos) || com.mmorpg.hub.ClassTrial.refuseTransfer(p, null)) return;
        MailData data = MailData.get(p.level().getServer());
        switch (action) {
            case ACT_SEND -> send(p, data, amount, text == null ? "" : text);
            case ACT_CLAIM -> {
                MailData.Letter l = data.find(p.getUUID(), amount);
                if (l != null) claim(p, data, l);
            }
            case ACT_CLAIM_ALL -> {
                for (MailData.Letter l : new ArrayList<>(data.box(p.getUUID()))) claim(p, data, l);
            }
            default -> {
            }
        }
        open(p, pos);
    }

    private static void warn(ServerPlayer p, String msg) {
        p.sendOverlayMessage(Component.literal(msg).withColor(0xFF7060));
    }

    private static void send(ServerPlayer p, MailData data, long gold, String text) {
        MinecraftServer server = p.level().getServer();
        String[] f = text.split(String.valueOf(SEP), -1);
        String toName = f.length > 0 ? f[0].strip() : "";
        String message = f.length > 1 ? f[1].strip() : "";
        String picks = f.length > 2 ? f[2].strip() : "";
        if (message.length() > MESSAGE_MAX) message = message.substring(0, MESSAGE_MAX);
        UUID to = null;
        ServerPlayer online = toName.isEmpty() ? null : server.getPlayerList().getPlayerByName(toName);
        if (online != null) {
            to = online.getUUID();
            toName = online.getGameProfile().name();
            data.remember(to, toName);
        } else if (!toName.isEmpty()) {
            to = data.lookup(toName);
        }
        if (to == null) {
            warn(p, "Destinataire inconnu : « " + toName + " » (il doit s'être déjà connecté).");
            return;
        }
        if (to.equals(p.getUUID())) {
            warn(p, "Vous ne pouvez pas vous écrire à vous-même.");
            return;
        }
        if (gold < 0 || gold > MAX_GOLD) {
            warn(p, "Montant invalide (0 à " + MAX_GOLD + " or).");
            return;
        }
        var bags = com.mmorpg.server.Backpacks.inventory(p);
        int size = 36 + com.mmorpg.server.Backpacks.capacity(bags);
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> taken = new ArrayList<>();
        if (!picks.isEmpty()) {
            for (String pick : picks.split(",")) {
                String[] sp = pick.split(":", 2);
                int slot;
                try {
                    slot = Integer.parseInt(sp[0]);
                } catch (NumberFormatException e) {
                    return;
                }
                if (slot < 0 || slot >= size || slots.contains(slot)) return;
                ItemStack held = slot < 36 ? p.getInventory().getItem(slot) : bags.getItem(slot - 34);
                if (held.isEmpty()) continue;
                if (sp.length > 1 && !sp[1].equals(Auction.saleFingerprint(held, p.level().registryAccess()))) return;
                if (CrateOnly.is(held.getItem())) {
                    warn(p, "Cet objet ne peut pas être envoyé.");
                    return;
                }
                slots.add(slot);
                taken.add(held.copy());
            }
        }
        if (taken.size() > MAX_ITEMS) {
            warn(p, "Quatre objets au maximum par lettre.");
            return;
        }
        if (gold == 0 && taken.isEmpty() && message.isEmpty()) {
            warn(p, "La lettre est vide.");
            return;
        }
        if (data.count(to) >= MAX_LETTERS) {
            warn(p, "La boîte de " + toName + " est pleine.");
            return;
        }
        RpgPlayers.depositCoins(p);
        if (!RpgPlayers.takeGold(p, gold)) {
            warn(p, "Or insuffisant (" + gold + " requis).");
            return;
        }
        for (int slot : slots) {
            if (slot < 36) p.getInventory().setItem(slot, ItemStack.EMPTY);
            else bags.setItem(slot - 34, ItemStack.EMPTY);
        }
        deliver(server, to, p.getUUID(), p.getGameProfile().name(), message, gold, taken, false);
        StringBuilder what = new StringBuilder();
        for (ItemStack s : taken) what.append(s.getHoverName().getString()).append(" x").append(s.getCount()).append(", ");
        com.mmorpg.db.Audit.log(p, com.mmorpg.db.Audit.MAIL_SEND, gold, "à " + toName + " (" + to + ") : " + what + "« " + message + " »");
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.8f, 1.2f);
        p.sendOverlayMessage(Component.literal("Lettre envoyée à " + toName + ".").withColor(0xFFD040));
    }

    private static void claim(ServerPlayer p, MailData data, MailData.Letter l) {
        long gold = l.gold();
        if (gold > 0) RpgPlayers.transferGold(p, gold, false);
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack s : l.items()) {
            ItemStack rest = s.copy();
            p.getInventory().add(rest);
            if (!rest.isEmpty()) left.add(rest);
        }
        if (left.isEmpty()) {
            data.remove(p.getUUID(), l);
        } else {
            data.replace(p.getUUID(), l, new MailData.Letter(l.id(), l.from(), l.fromName(), l.message(), 0, left, l.sent(), l.system()));
            warn(p, "Inventaire plein : des objets restent dans la lettre.");
        }
        if (gold > 0 || left.size() < l.items().size()) {
            com.mmorpg.db.Audit.log(p, com.mmorpg.db.Audit.MAIL_CLAIM, gold, "lettre de " + l.fromName() + " (" + l.items().size() + " objet(s), " + left.size() + " restant(s))");
            p.level().playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.COINS.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        }
    }
}
