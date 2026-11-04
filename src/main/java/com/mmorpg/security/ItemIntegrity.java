package com.mmorpg.security;

import com.mmorpg.MMORPG;
import com.mmorpg.admin.PlayerAdmin;
import com.mmorpg.anticheat.AntiCheat;
import com.mmorpg.db.Audit;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Detection de duplication d'objets. Chaque equipement du mod (non empilable) recoit un identifiant unique {@code mmorpg_uid} la premiere fois
 * qu'on le voit dans un inventaire ou un coffre d'Ender. Le meme identifiant present deux fois chez les joueurs connectes, sur deux balayages
 * consecutifs (un echange en cours n'est donc pas pris pour une copie), est signale aux operateurs et journalise. Rien n'est supprime
 * automatiquement. Les copies faites par un operateur sont ignorees.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ItemIntegrity {
    private ItemIntegrity() {
    }

    public static final String TAG = "mmorpg_uid";
    private static final int PERIOD_TICKS = 100;
    private static final Map<String, Integer> STRIKES = new HashMap<>();
    private static final Set<String> REPORTED = new HashSet<>();

    /** Une copie detectee. */
    public record Dupe(String uid, String item, List<String> holders) {
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.getTickCount() % PERIOD_TICKS != 0) return;
        if (!com.mmorpg.server.FeatureFlags.enabled(s, "anticheat")) return;
        for (Dupe d : scan(s.getPlayerList().getPlayers())) {
            AntiCheat.alert("§c[Anticheat] Objet dupliqué : " + d.item() + " chez " + String.join(", ", d.holders()));
            Audit.log((UUID) null, "anticheat", Audit.ANTICHEAT, 0, "duplication " + d.item() + " uid " + d.uid() + " : " + String.join(", ", d.holders()));
            com.mmorpg.MMORPG.LOGGER.warn("[MMORPG] ANTICHEAT : duplication {} uid {} : {}", d.item(), d.uid(), d.holders());
        }
    }

    /** Un balayage ; renvoie les copies confirmees (vues sur deux balayages de suite) et pas encore signalees. */
    public static synchronized List<Dupe> scan(List<ServerPlayer> players) {
        Map<String, List<String>> holders = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        Set<String> exempt = new HashSet<>();
        for (ServerPlayer p : players) {
            boolean op = PlayerAdmin.allowed(p) || p.isCreative();
            visit(p.getInventory(), p, op, holders, names, exempt);
            visit(p.getEnderChestInventory(), p, op, holders, names, exempt);
        }
        List<Dupe> out = new ArrayList<>();
        Set<String> dup = new HashSet<>();
        for (var en : holders.entrySet()) {
            if (en.getValue().size() < 2 || exempt.contains(en.getKey())) continue;
            dup.add(en.getKey());
            int n = STRIKES.merge(en.getKey(), 1, Integer::sum);
            if (n >= 2 && REPORTED.add(en.getKey())) out.add(new Dupe(en.getKey(), names.get(en.getKey()), en.getValue()));
        }
        STRIKES.keySet().retainAll(dup);
        REPORTED.retainAll(dup);
        return out;
    }

    private static void visit(Container c, ServerPlayer p, boolean op, Map<String, List<String>> holders, Map<String, String> names, Set<String> exempt) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack st = c.getItem(i);
            if (st.isEmpty() || st.getMaxStackSize() > 1) continue;
            var id = BuiltInRegistries.ITEM.getKey(st.getItem());
            if (!MMORPG.MODID.equals(id.getNamespace())) continue;
            String uid = uidOf(st);
            if (uid == null) {
                if (!op) stamp(st);
                continue;
            }
            holders.computeIfAbsent(uid, k -> new ArrayList<>()).add(p.getGameProfile().name() + "@" + i);
            names.put(uid, id.toString());
            if (op) exempt.add(uid);
        }
    }

    public static String uidOf(ItemStack st) {
        CustomData d = st.get(DataComponents.CUSTOM_DATA);
        return d == null ? null : d.copyTag().getStringOr(TAG, null);
    }

    public static void stamp(ItemStack st) {
        String uid = UUID.randomUUID().toString();
        CustomData.update(DataComponents.CUSTOM_DATA, st, t -> t.putString(TAG, uid));
    }
}
