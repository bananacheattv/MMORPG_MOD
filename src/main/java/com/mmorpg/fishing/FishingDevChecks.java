package com.mmorpg.fishing;

import com.mmorpg.MMORPG;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.item.AeloriaRodItem;
import com.mmorpg.item.TreasureChestItem;
import com.mmorpg.job.Job;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;

/** Verifications de la peche dans un vrai serveur : ./gradlew runServer -PfishingChecks (monde isole run/). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class FishingDevChecks {
    private FishingDevChecks() {
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("[FISHING CHECKS] " + message);
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory()) if (s.is(item)) n += s.getCount();
        return n;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void run(ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("mmorpg.checkFishing")) return;
        Files.deleteIfExists(Path.of("../build/reports/fishing-checks.txt"));
        var server = event.getServer();
        ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "FishingChecks"));
        p.setData(ModAttachments.PLAYER, new PlayerData());
        PlayerData d = RpgPlayers.get(p);
        Random rnd = new Random(12345);

        // --- enregistrement : chaque prise, ingredient et canne est un objet du mod
        int registered = 0;
        for (FishCatalog.Entry e : FishCatalog.ALL) {
            Item item = ModItems.get(e.id());
            check(item != null && item != net.minecraft.world.item.Items.AIR, "objet manquant " + e.id());
            check(BuiltInRegistries.ITEM.getKey(item).equals(MMORPG.id(e.id())), "identifiant " + e.id());
            check(e.kind() != FishCatalog.Kind.TRESOR || item instanceof TreasureChestItem, "coffre " + e.id());
            registered++;
        }
        for (FishCatalog.Ingredient i : FishCatalog.INGREDIENTS) check(ModItems.get(i.id()) != null, "ingredient manquant " + i.id());
        for (FishCatalog.Rod r : FishCatalog.RODS) {
            Item item = ModItems.get(r.id());
            check(item instanceof AeloriaRodItem rod && rod.rod().tier() == r.tier(), "canne " + r.id());
            check(new ItemStack(item).getMaxDamage() == r.durability(), "durabilite " + r.id());
            var recipe = ForgeRecipes.get(r.id());
            check(recipe != null && recipe.resultItem() == item, "recette de canne " + r.id());
            for (var ing : recipe.ingredients()) check(ing.resolve() != net.minecraft.world.item.Items.AIR, "ingredient de " + r.id() + " : " + ing.item());
            Job.Gate gate = Job.gateFor(r.id());
            check(r.minJobLevel() <= 1 ? gate == null : gate != null && gate.level() == r.minJobLevel() && gate.job() == Job.PECHEUR, "verrou de recette " + r.id());
        }
        for (int rank = 0; rank < 4; rank++) {
            for (TreasureLoot.Drop drop : TreasureLoot.table(rank)) check(ModItems.get(drop.itemId()) != null, "recompense inconnue " + drop.itemId());
        }
        for (String dish : new String[]{"filet_grille", "ragout_des_profondeurs", "festin_du_leviathan"}) {
            for (var ing : ForgeRecipes.get(dish).ingredients()) {
                check(FishCatalog.get(ing.item()) != null || FishCatalog.ingredient(ing.item()) != null, "plat de peche sans prise du mod " + dish + ":" + ing.item());
            }
        }

        // --- une prise ramenee : objet, journal, experience, succes
        d.jobXp.put("pecheur", Job.totalFor(30));
        FishCatalog.Entry carpe = FishCatalog.get("carpe_doree");
        FishingRules.Context ctx = new FishingRules.Context(30, 4, FishCatalog.Habitat.LAC, false, false);
        long xpBefore = d.jobXp.get("pecheur");
        FishingEvents.award(p, carpe, ctx, true, rnd);
        check(count(p, ModItems.get("carpe_doree")) >= 1, "carpe donnee");
        check(d.fishCaught.getOrDefault("carpe_doree", 0) >= 1 && d.fishBest.getOrDefault("carpe_doree", 0) >= carpe.minCm(), "journal de peche");
        check(d.jobXp.get("pecheur") > xpBefore, "experience de peche");

        // --- le mini-jeu bout en bout : touche, clic au centre, prise ramenee
        FishCatalog.Entry saumon = FishCatalog.get("saumon_royal");
        FishingEvents.Session s = FishingEvents.start(p, new FishingRules.Context(30, 4, FishCatalog.Habitat.RIVIERE, false, false), saumon, rnd, 4);
        check(s != null && FishingEvents.session(p) == s, "session de touche ouverte");
        long now = server.getTickCount();
        int caughtBefore = d.fishCaught.getOrDefault("saumon_royal", 0);
        FishFight f = s.fight;
        int guard = 0;
        while (!f.finished() && guard++ < 10) {
            long best = f.roundStart();
            double bd = 9;
            for (long t = 0; t < f.period; t++) {
                double dist = Math.abs(FishFight.cursor(t, f.period, f.currentPhase()) - f.currentCenter());
                if (dist < bd) {
                    bd = dist;
                    best = f.roundStart() + t;
                }
            }
            check(FishingEvents.press(p, s, best, 0) == FishFight.Press.PERFECT, "clic au centre de la zone");
        }
        check(FishingEvents.session(p) == null, "session terminee");
        check(d.fishCaught.getOrDefault("saumon_royal", 0) > caughtBefore, "saumon royal ramene");

        // --- une touche ratee : rien dans l'inventaire, un petit gain d'experience
        FishCatalog.Entry gardon = FishCatalog.get("gardon_argente");
        FishingEvents.Session miss = FishingEvents.start(p, new FishingRules.Context(30, 4, FishCatalog.Habitat.LAC, false, false), gardon, rnd, 4);
        int before = d.fishCaught.getOrDefault("gardon_argente", 0);
        long xp = d.jobXp.get("pecheur");
        for (long t = 0; t < miss.fight.period; t++) {
            double dist = Math.abs(FishFight.cursor(t, miss.fight.period, miss.fight.currentPhase()) - miss.fight.currentCenter());
            if (dist > miss.fight.halfWidth + 0.2) {
                FishingEvents.press(p, miss, miss.fight.roundStart() + t, 0);
                break;
            }
        }
        check(FishingEvents.session(p) == null && d.fishCaught.getOrDefault("gardon_argente", 0) == before, "touche ratee : la prise s'echappe");
        check(d.jobXp.get("pecheur") == xp + FishCatalog.ESCAPE_XP, "experience de consolation");

        // --- un dechet est remonte sans mini-jeu
        check(FishingEvents.start(p, ctx, FishCatalog.get("botte_rouillee"), rnd, 4) == null && count(p, ModItems.get("botte_rouillee")) >= 1, "dechet direct");

        // --- ouverture des coffres : or et objets, le coffre est consomme
        for (FishCatalog.Entry chest : FishCatalog.of(FishCatalog.Kind.TRESOR)) {
            p.getInventory().clearContent();
            p.getInventory().add(new ItemStack(ModItems.get(chest.id())));
            long gold = d.gold;
            ItemStack held = p.getInventory().getItem(0);
            p.setItemInHand(InteractionHand.MAIN_HAND, held);
            ModItems.get(chest.id()).use(p.level(), p, InteractionHand.MAIN_HAND);
            check(d.gold > gold, "or du coffre " + chest.id());
            check(count(p, ModItems.get(chest.id())) == 0, "coffre consomme " + chest.id());
            int items = 0;
            for (ItemStack st : p.getInventory()) if (!st.isEmpty()) items++;
            check(items >= 1, "objets du coffre " + chest.id());
        }

        // --- la canne exige son niveau de metier
        d.jobXp.put("pecheur", Job.totalFor(1));
        ItemStack rod6 = new ItemStack(ModItems.get("canne_du_leviathan"));
        p.setItemInHand(InteractionHand.MAIN_HAND, rod6);
        var rc = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
        check(rc.isCanceled(), "canne du Leviathan refusee au niveau 1");
        d.jobXp.put("pecheur", Job.totalFor(50));
        rc = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
        check(!rc.isCanceled(), "canne du Leviathan autorisee au niveau 50");

        // --- persistance du journal
        PlayerData copy = new PlayerData();
        copy.load(d.save());
        check(copy.fishCaught.equals(d.fishCaught) && copy.fishBest.equals(d.fishBest), "sauvegarde du journal de peche");

        MMORPG.LOGGER.info("[FISHING CHECKS] PASS: {} prises, {} cannes, mini-jeu, coffres, journal, verrous", registered, FishCatalog.RODS.size());
        Files.createDirectories(Path.of("../build/reports"));
        Files.writeString(Path.of("../build/reports/fishing-checks.txt"), "PASS " + registered + " catches\n");
        server.halt(false);
    }
}
