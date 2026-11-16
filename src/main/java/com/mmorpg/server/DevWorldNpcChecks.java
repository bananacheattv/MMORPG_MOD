package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.hub.SpawnBuilder;
import com.mmorpg.quest.WorldNpcs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

/** Verification en jeu (opt-in, -Dmmorpg.checkWorldNpcs=true) : les 230 PNJ de quetes du spawn et du monde ouvert sont poses, une seule fois, sur le sol. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevWorldNpcChecks {
    @SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkWorldNpcs")) return;
        var server = event.getServer();
        var early = new ArrayList<String>();
        deliveries(server, early);
        MMORPG.LOGGER.info("[WORLD NPC CHECKS] livraisons {}", early.isEmpty() ? "PASS" : "FAIL " + early);
        var data = com.mmorpg.hub.SpawnData.get(server);
        var spawn = server.getLevel(SpawnBuilder.DIM);
        boolean spawnOk = spawn != null && data.exists();
        if (spawnOk) {
            var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(spawn, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "WorldNpcChecks"));
            var city = data.origin();
            for (var e : WorldNpcs.entries()) if (e.dim().equals("spawn")) visit(fake, spawn, city.getX() + com.mmorpg.hub.HubMarket.CX + e.dx(), city.getZ() + e.dz(), (int) data.arrival().y);
        } else MMORPG.LOGGER.warn("[WORLD NPC CHECKS] spawn absent : seul le monde ouvert est verifie");
        var ow = server.overworld();
        var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(ow, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "WorldNpcChecks2"));
        var list = WorldNpcs.entries().stream().filter(e -> e.dim().equals("openworld")).toList();
        int[] i = {0, 0, 0};        // index, passe, premier decompte
        Runnable[] step = new Runnable[1];
        step[0] = () -> {
            if (i[0] >= list.size()) {
                if (i[1] == 0) { i[1] = 1; i[0] = 0; i[2] = count(server); MMORPG.LOGGER.info("[WORLD NPC CHECKS] passe 1 terminee : {} PNJ", i[2]); }
                else { int second = count(server); MMORPG.LOGGER.info("[WORLD NPC CHECKS] idempotence : {} puis {} PNJ", i[2], second);
                    if (i[2] != second) MMORPG.LOGGER.error("[WORLD NPC CHECKS] FAIL doublons apres une seconde passe"); finish(server, spawnOk); return; }
            }
            var e = list.get(i[0]++); var a = WorldNpcs.anchor(e.region());
            visit(fake, ow, a[0] + e.dx(), a[1] + e.dz(), 0);
            Scheduler.later(ow, 2, step[0]);
        };
        Scheduler.later(ow, 20, step[0]);
    }

    private static int count(net.minecraft.server.MinecraftServer server) {
        int total = 0;
        for (var t : com.mmorpg.world.NpcDirectory.get(server).snapshot()) if (t instanceof net.minecraft.nbt.CompoundTag n && WorldNpcs.GROUP.equals(n.getStringOr("group", ""))) total++;
        return total;
    }

    private static void visit(ServerPlayer fake, net.minecraft.server.level.ServerLevel level, int x, int z, int y) {
        for (int cx = (x >> 4) - 2; cx <= (x >> 4) + 2; cx++) for (int cz = (z >> 4) - 2; cz <= (z >> 4) + 2; cz++) level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        fake.snapTo(x + .5, y == 0 ? level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) : y, z + .5);
        WorldNpcs.install(fake);
    }

    private static void finish(net.minecraft.server.MinecraftServer server, boolean spawnChecked) {
        var fails = new ArrayList<String>();
        var directory = com.mmorpg.world.NpcDirectory.get(server);
        var quests = com.mmorpg.config.ConfigManager.quests();
        int spawnN = 0, worldN = 0;
        var names = new HashSet<String>();
        for (var e : WorldNpcs.entries()) {
            if (e.dim().equals("spawn") && !spawnChecked) continue;
            var entry = directory.entry(e.uuid().toString());
            if (entry == null) { fails.add("absent " + e.name() + " (" + e.region() + ")"); continue; }
            var q = quests.get(e.quest());
            if (q == null || !e.uuid().toString().equals(q.npc)) fails.add("quete non liee " + e.quest());
            if (!names.add(e.name())) fails.add("nom en double " + e.name());
            if (e.dim().equals("spawn")) spawnN++; else worldN++;
            var at = BlockPos.of(entry.getLongOr("pos", 0));
            MMORPG.LOGGER.info("[WORLD NPC CHECKS] {} | {} | {} @ {}", e.region(), e.name(), e.quest(), at);
        }
        deliveries(server, fails);
        int total = 0;
        for (var l : server.getAllLevels()) for (var en : l.getAllEntities()) if (en instanceof NpcEntity n && WorldNpcs.GROUP.equals(n.group())) total++;
        MMORPG.LOGGER.info("[WORLD NPC CHECKS] {} : {} spawn, {} monde ouvert dans l'annuaire, {} entites chargees", fails.isEmpty() ? "PASS" : "FAIL " + fails.subList(0, Math.min(10, fails.size())), spawnN, worldN, total);
    }
    /** Quetes de recolte / livraison : chaque objet demande existe, la quete se valide avec les objets en main et les consomme. */
    private static void deliveries(net.minecraft.server.MinecraftServer server, List<String> fails) {
        var ow = server.overworld();
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(ow, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "DeliveryChecks"));
        var d = RpgPlayers.get(p);
        int n = 0;
        for (var e : com.mmorpg.config.ConfigManager.quests().entrySet()) {
            var q = e.getValue();
            if (q.lore == null) continue;
            for (var o : q.objectives) {
                if (!o.type.equals("collect")) continue;
                n++;
                var item = com.mmorpg.crafting.ForgeRecipes.resolveItem(o.target);
                if (item == null || item == net.minecraft.world.item.Items.AIR) { fails.add("objet inconnu " + e.getKey() + " " + o.target); continue; }
                if (!q.lore.objective.contains(String.valueOf(o.count))) fails.add("texte sans quantite " + e.getKey());
                p.getInventory().clearContent();
                d.activeQuests.clear();
                d.activeQuests.put(e.getKey(), new int[]{0});
                if (QuestManager.completable(p, d, e.getKey())) fails.add("completable sans objets " + e.getKey());
                p.getInventory().add(new net.minecraft.world.item.ItemStack(item, o.count));
                if (!QuestManager.completable(p, d, e.getKey())) fails.add("non completable avec les objets " + e.getKey());
            }
        }
        p.getInventory().clearContent();
        d.activeQuests.clear();
        MMORPG.LOGGER.info("[WORLD NPC CHECKS] livraisons : {} quetes de recolte verifiees", n);
    }
}
