package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Vitrine "ui_frames" : ouvre chaque interface du mod et la capture, pour verifier qu'elles ont toutes le cadre de l'inventaire. */
final class DevUiFrames {
    private static int tick = -1;
    private static final List<String> NAMES = new ArrayList<>();
    private static final List<Supplier<Screen>> SCREENS = new ArrayList<>();

    private static void add(String name, Supplier<Screen> s) {
        NAMES.add(name);
        SCREENS.add(s);
    }

    private static Screen adminPlayers(int page, String... path) {
        var data = new com.mmorpg.rpg.PlayerData();
        data.playerClass = com.mmorpg.rpg.PlayerClass.MAGE;
        data.level = 42;
        data.gold = 18250;
        data.skillRanks.put("meteor", 3);
        data.skillRanks.put("blizzard", 2);
        data.jobXp.put("mineur", 4200L);
        var tag = new CompoundTag();
        tag.putString("kind", "profile");
        tag.putString("id", "00000000-0000-0000-0000-000000000001");
        tag.putString("name", "Aelwyn");
        tag.putBoolean("online", true);
        tag.put("data", data.save());
        tag.putLong("bankGold", 12500);
        var inv = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 6; i++) {
            var e = new CompoundTag();
            e.putInt("slot", i);
            e.putString("item", i % 2 == 0 ? "minecraft:diamond" : "mmorpg:verdite");
            e.putInt("count", 8 + i);
            inv.add(e);
        }
        tag.put("inventory", inv);
        var list = new CompoundTag();
        list.putString("kind", "list");
        var players = new net.minecraft.nbt.ListTag();
        String[][] ps = {{"Aelwyn", "MAGE", "42", "1"}, {"Borin", "GUERRIER", "17", "1"}, {"Cyra", "ARCHER", "63", "0"}};
        for (int i = 0; i < ps.length; i++) {
            var e = new CompoundTag();
            e.putString("id", "00000000-0000-0000-0000-00000000000" + (i + 1));
            e.putString("name", ps[i][0]);
            e.putString("class", ps[i][1]);
            e.putInt("level", Integer.parseInt(ps[i][2]));
            e.putBoolean("online", ps[i][3].equals("1"));
            players.add(e);
        }
        list.put("players", players);
        var screen = new AdminPlayersScreen(list);
        screen.receive(tag);
        return screen.preview(page, path);
    }

    static {
        add("inventory", () -> new CharacterScreen(Minecraft.getInstance().player));
        add("pause", AeloriaPauseScreen::new);
        add("admin_players", () -> adminPlayers(0));
        add("admin_players_data", () -> adminPlayers(1, "skills"));
        add("admin_players_items", () -> adminPlayers(2));
        add("admin_players_actions", () -> adminPlayers(3));
        add("jobs", JobsScreen::new);
        add("achievements", AchievementsScreen::new);
        add("skills", SkillsScreen::new);
        add("quests", QuestLogScreen::new);
        add("party", PartyScreen::new);
        add("pets", PetsScreen::new);
        add("mounts", MountsScreen::new);
        add("cosmetics", CosmeticsScreen::new);
        add("bestiary", BestiaryScreen::new);
        add("class_select", ClassSelectScreen::new);
        add("weekly", () -> {
            var data = new CompoundTag();
            data.putLong("reset", 3 * 86400L + 4 * 3600L);
            data.putInt("doneCount", 1);
            var tiers = new net.minecraft.nbt.ListTag();
            String[] names = {"Facile", "Difficile", "Très difficile"};
            for (int i = 0; i < 3; i++) {
                var q = new CompoundTag();
                q.putInt("tier", i);
                q.putInt("state", i);
                q.putBoolean("completable", i == 1);
                q.putString("tierName", names[i]);
                q.putString("name", "Chasseur de la semaine");
                var objs = new net.minecraft.nbt.ListTag();
                var o = new CompoundTag();
                o.putString("type", i == 2 ? "collect" : "kill");
                o.putString("target", i == 2 ? "minecraft:diamond" : "*");
                o.putInt("count", 10 * (i + 1));
                o.putInt("progress", 4 * i);
                objs.add(o);
                q.put("objectives", objs);
                var rw = new CompoundTag();
                rw.putLong("gold", 500L * (i + 1));
                rw.putLong("xp", 1200L * (i + 1));
                q.put("rewards", rw);
                tiers.add(q);
            }
            data.put("tiers", tiers);
            return new WeeklyQuestScreen(data);
        });
        add("trade", () -> {
            var data = new CompoundTag();
            data.putString("partner", "Aelwyn");
            data.putString("status", "En attente de confirmation");
            data.putLong("myGold", 250);
            data.putLong("theirGold", 100);
            data.putLong("wallet", 1800);
            return new TradeScreen(data);
        });
        add("bank", () -> {
            var data = new CompoundTag();
            data.putLong("bank", 12500);
            data.putLong("wallet", 830);
            data.putInt("level", 3);
            return new BankScreen(data);
        });
        add("forge", () -> new ForgeScreen(net.minecraft.core.BlockPos.ZERO));
        add("dungeons", () -> {
            var data = new CompoundTag();
            data.putInt("playerLevel", 30);
            var list = new net.minecraft.nbt.ListTag();
            String[][] ds = {{"crypte_gobeline", "Crypte gobeline", "10"}, {"catacombes", "Catacombes", "30"}, {"forteresse_infernale", "Forteresse infernale", "50"}};
            for (var d : ds) {
                var e = new CompoundTag();
                e.putString("id", d[0]);
                e.putString("label", d[1]);
                e.putInt("level", Integer.parseInt(d[2]));
                e.putString("lore", "Un donjon rien que pour vous et votre groupe.");
                for (int k = 0; k < 3; k++) e.putString("variant" + k, "Variante " + k);
                list.add(e);
            }
            data.put("dungeons", list);
            return new DungeonMasterScreen(data);
        });
        add("auction", () -> {
            var screen = new AuctionScreen(auctionData());
            screen.previewTab(0);
            return screen;
        });
        add("auction_sell", () -> {
            var screen = new AuctionScreen(auctionData());
            screen.previewTab(1);
            return screen;
        });
        add("auction_mine", () -> {
            var screen = new AuctionScreen(auctionData());
            screen.previewTab(2);
            return screen;
        });
        add("store", () -> new PremiumStoreScreen(DevShowcase.expansionStore()));
        add("lucky", () -> {
            var data = new CompoundTag();
            data.putInt("tier", 2);
            data.put("table", com.mmorpg.server.AdventureLoot.preview(com.mmorpg.block.LuckyTier.of(2).id));
            return new LuckyBlockScreen(data);
        });
        add("arena", () -> {
            var data = new CompoundTag();
            data.putInt("bracket", 2);
            return new ArenaScreen(data);
        });
        add("guild", () -> {
            var guild = new com.mmorpg.guild.GuildStore.Guild();
            guild.name = "Les Veilleurs d’Aeloria";
            guild.level = 4;
            guild.gold = 25000;
            var self = new java.util.UUID(0, 1);
            guild.members.put(self, new com.mmorpg.guild.GuildStore.Member(self, "Aeloria", "CHEF"));
            var data = new CompoundTag();
            data.putString("self", self.toString());
            data.putString("guild", new com.google.gson.Gson().toJson(guild));
            return new GuildScreen(data);
        });
    }

    private static CompoundTag auctionData() {
        var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
        net.minecraft.world.item.Item[] items = {net.minecraft.world.item.Items.NETHERITE_SWORD, net.minecraft.world.item.Items.BOW, net.minecraft.world.item.Items.DIAMOND_AXE,
                net.minecraft.world.item.Items.NETHERITE_CHESTPLATE, net.minecraft.world.item.Items.OAK_PLANKS, net.minecraft.world.item.Items.GOLDEN_APPLE,
                net.minecraft.world.item.Items.TRIDENT, net.minecraft.world.item.Items.CROSSBOW, net.minecraft.world.item.Items.ENDER_PEARL, net.minecraft.world.item.Items.SHIELD};
        var data = new CompoundTag();
        data.putLong("wallet", 54475);
        data.putInt("pages", 2);
        data.putInt("total", 12);
        for (String key : new String[]{"listings", "mine"}) {
            var list = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < (key.equals("mine") ? 3 : items.length); i++) {
                var e = new CompoundTag();
                e.putLong("id", i + 1);
                e.putString("seller", "1Touchable");
                e.putLong("price", 315L * (i + 1) * (i + 1));
                e.putLong("left", (47L - i) * 3_600_000L);
                e.put("stack", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.encodeStart(ops, new net.minecraft.world.item.ItemStack(items[i], i == 4 ? 32 : 1)).getOrThrow());
                list.add(e);
            }
            data.put(key, list);
        }
        return data;
    }

    private DevUiFrames() {
    }

    /** Tailles de fenetre et echelles d'interface testees (0 = automatique). */
    private static final int[][] CONFIGS = {{1280, 720, 2}, {1280, 720, 1}, {1280, 720, 3}, {1280, 720, 4}, {1280, 720, 0}, {854, 480, 2}, {854, 480, 0}, {1920, 1080, 0}, {1920, 1080, 3}};
    private static int cfg = -1, cfgTick, reference[];
    private static final List<String> REPORT = new ArrayList<>();
    private static int gaps;

    private static int[] frameOf(Screen s) {
        if (s instanceof com.mmorpg.client.ui.MmoScreen m) return m.realFrame();
        if (s instanceof CharacterScreen c) return c.realFrame();
        return null;
    }

    static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        tick++;
        if (tick < 120) return;
        int per = 8;
        if (cfg < 0 || cfgTick >= SCREENS.size() * per) {
            cfg++;
            cfgTick = 0;
            reference = null;
            if (cfg >= CONFIGS.length) {
                REPORT.add(gaps == 0 ? "OK : tous les ecrans ont le meme cadre et la meme barre" : "ECARTS : " + gaps);
                try {
                    java.nio.file.Files.write(mc.gameDirectory.toPath().resolve("ui_frames_report.txt"), REPORT);
                } catch (java.io.IOException e) {
                    MMORPG.LOGGER.error("[MMORPG][ui_frames] rapport : {}", e.toString());
                }
                MMORPG.LOGGER.info("[MMORPG][ui_frames] {}", REPORT.get(REPORT.size() - 1));
                DevShowcase.stop(mc);
                return;
            }
            var c = CONFIGS[cfg];
            mc.getWindow().setWindowed(c[0], c[1]);
            mc.options.guiScale().set(c[2]);
            mc.resizeGui();
            REPORT.add("== fenetre " + c[0] + "x" + c[1] + " echelle " + (c[2] == 0 ? "auto" : c[2]) + " (interface " + mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight() + ")");
            return;
        }
        int i = cfgTick / per, step = cfgTick % per;
        cfgTick++;
        if (step == 0) {
            try {
                mc.gui.setScreen(SCREENS.get(i).get());
            } catch (Throwable t) {
                MMORPG.LOGGER.error("[MMORPG][ui_frames] {} : {}", NAMES.get(i), t.toString());
            }
        }
        if (step == 5) {
            Screen s = mc.gui.screen();
            int[] f = frameOf(s);
            if (f == null) {
                REPORT.add("  " + NAMES.get(i) + " : ecran non mesurable");
            } else {
                if (reference == null) reference = f;
                // meme position, meme taille, meme barre (les ecrans sans barre ne comparent que le cadre)
                boolean same = f[0] == reference[0] && f[1] == reference[1] && f[2] == reference[2] && f[3] == reference[3] && (f[4] == 0 || f[4] == reference[4]);
                if (!same) gaps++;
                REPORT.add("  " + NAMES.get(i) + " : cadre " + f[2] + "x" + f[3] + " en (" + f[0] + "," + f[1] + ") barre " + f[4] + (same ? "" : "   <-- ECART"));
            }
            if (cfg == 0) DevShowcase.shot(mc, "frame_" + NAMES.get(i));
        }
    }
}
