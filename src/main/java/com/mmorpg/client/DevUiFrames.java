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

    static {
        add("inventory", () -> new CharacterScreen(Minecraft.getInstance().player));
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
        add("weekly", () -> new WeeklyQuestScreen(new CompoundTag()));
        add("auction", () -> new AuctionScreen(new CompoundTag()));
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

    private DevUiFrames() {
    }

    static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        tick++;
        if (tick == 30) {
            mc.getWindow().setWindowed(1280, 720);
            mc.options.guiScale().set(2);
            mc.resizeGui();
        }
        if (tick < 120) return;
        int frame = tick - 120, i = frame / 12;
        if (i >= SCREENS.size()) {
            DevShowcase.stop(mc);
            return;
        }
        if (frame % 12 == 0) {
            try {
                mc.gui.setScreen(SCREENS.get(i).get());
            } catch (Throwable t) {
                MMORPG.LOGGER.error("[MMORPG][ui_frames] {} : {}", NAMES.get(i), t.toString());
            }
        }
        if (frame % 12 == 8) {
            Screen s = mc.gui.screen();
            if (s instanceof com.mmorpg.client.ui.MmoScreen m)
                MMORPG.LOGGER.info("[MMORPG][ui_frames] {} : echelle {} cadre {}x{} (ecran {}x{})", NAMES.get(i), m.uiScale(), m.panelWidthPublic(), m.panelHeightPublic(), s.width, s.height);
            DevShowcase.shot(mc, "frame_" + NAMES.get(i));
        }
    }
}
