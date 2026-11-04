package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.registry.ModItems;
import com.mmorpg.security.AdminJournal;
import com.mmorpg.security.CommandGuard;
import com.mmorpg.security.ItemIntegrity;
import com.mmorpg.security.PacketSigner;
import com.mmorpg.security.ScreenGuard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.List;
import java.util.UUID;

/** Controles serveur de la securite : ./gradlew runServer -PsecurityChecks. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevSecurityChecks {
    private DevSecurityChecks() {
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError("Security check failed: " + label);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkSecurity")) return;
        var server = event.getServer();
        var level = server.overworld();
        var a = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "SecA"));
        var b = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "SecB"));
        try {
            // limite de debit des commandes
            long t = System.nanoTime();
            int ok = 0;
            for (int i = 0; i < 20; i++) if (CommandGuard.take(a.getUUID(), t)) ok++;
            check(ok == 8, "rafale de commandes limitee a 8 (" + ok + ")");
            check(CommandGuard.take(a.getUUID(), t + 1_000_000_000L), "recharge apres une seconde");

            // interfaces
            check(!ScreenGuard.wasOpened(a.getUUID(), "shop_action", System.currentTimeMillis()), "boutique jamais ouverte refusee");
            check(ScreenGuard.wasOpened(a.getUUID(), "social_action", System.currentTimeMillis()), "paquet sans ecran libre");
            ScreenGuard.opened(a, S2COpenScreen.SHOP);
            check(ScreenGuard.wasOpened(a.getUUID(), "shop_action", System.currentTimeMillis()), "boutique ouverte acceptee");
            check(!ScreenGuard.wasOpened(a.getUUID(), "shop_action", System.currentTimeMillis() + ScreenGuard.class.getDeclaredFields().length * 0 + 31 * 60_000L), "ouverture expiree");
            check(!ScreenGuard.wasOpened(a.getUUID(), "teleport", System.currentTimeMillis()), "teleporteur jamais ouvert refuse");

            // signature
            var access = server.registryAccess();
            byte[] key = new byte[32];
            new java.security.SecureRandom().nextBytes(key);
            var shop = new Payloads.ShopAction(Payloads.ShopAction.BUY, 1, 5, 7);
            byte[] body = PacketSigner.encode("shop_action", shop, access);
            check(body != null, "paquet encodable");
            byte[] mac = PacketSigner.mac(key, 1, "shop_action", body);
            check(java.security.MessageDigest.isEqual(mac, PacketSigner.mac(key, 1, "shop_action", body)), "signature stable");
            byte[] other = PacketSigner.encode("shop_action", new Payloads.ShopAction(Payloads.ShopAction.BUY, 1, 99, 7), access);
            check(!java.security.MessageDigest.isEqual(mac, PacketSigner.mac(key, 1, "shop_action", other)), "contenu modifie => signature differente");
            check(!java.security.MessageDigest.isEqual(mac, PacketSigner.mac(key, 2, "shop_action", body)), "numero different => signature differente");
            CompoundTag t1 = new CompoundTag(), t2 = new CompoundTag();
            for (String k : List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t")) t1.putInt(k, k.charAt(0));
            for (String k : List.of("t", "s", "r", "q", "p", "o", "n", "m", "l", "k", "j", "i", "h", "g", "f", "e", "d", "c", "b", "a")) t2.putInt(k, k.charAt(0));
            check(java.util.Arrays.equals(PacketSigner.encode("social_action", new Payloads.SocialAction("bank", t1), access),
                    PacketSigner.encode("social_action", new Payloads.SocialAction("bank", t2), access)), "ordre des etiquettes sans effet");

            // journal d'audit
            String marker = "test-" + UUID.randomUUID();
            AdminJournal.write("SecA", "admin", 0, marker);
            check(AdminJournal.tail(5).stream().anyMatch(l -> l.contains(marker)), "journal d'audit relisible");

            // duplication
            ItemStack sword = new ItemStack(ModItems.get("marteau_runique"));
            a.getInventory().clearContent();
            b.getInventory().clearContent();
            a.getInventory().setItem(0, sword);
            check(ItemIntegrity.scan(List.of(a, b)).isEmpty() && ItemIntegrity.uidOf(a.getInventory().getItem(0)) != null, "objet marque");
            b.getInventory().setItem(3, a.getInventory().getItem(0).copy());
            check(ItemIntegrity.scan(List.of(a, b)).isEmpty(), "premiere vue d'une copie : pas encore signalee");
            var dupes = ItemIntegrity.scan(List.of(a, b));
            check(dupes.size() == 1 && dupes.get(0).holders().size() == 2, "copie confirmee au second balayage");
            check(ItemIntegrity.scan(List.of(a, b)).isEmpty(), "signalee une seule fois");
            b.getInventory().clearContent();
            check(ItemIntegrity.scan(List.of(a, b)).isEmpty(), "copie disparue");
            MMORPG.LOGGER.info("[CODEX CHECKS] SECURITY PASS: debit commandes, ecrans, signature, journal d'audit, duplication");
        } catch (Throwable e) {
            MMORPG.LOGGER.error("[CODEX CHECKS] SECURITY FAIL", e);
        } finally {
            if (Boolean.getBoolean("mmorpg.checkSecurityStop")) server.halt(false);
        }
    }
}
