package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.fishing.FishFight;
import com.mmorpg.rpg.Rarity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FishingRodItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Jauge du mini-jeu de peche : une aiguille fait des allers-retours, la zone coloree marque l'instant ou cliquer. Dessine la meme aiguille
 * que celle que le serveur juge (FishFight), a partir de la graine et du delai recus.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class FishFightClient {
    public static final Identifier LAYER = MMORPG.id("fish_fight");
    private static final long TICK_NANOS = 50_000_000L;
    private static final long RESULT_NANOS = 2_200_000_000L;
    /** Sans nouvelles du serveur ROUND_TIMEOUT (+ marge) apres le debut de la manche, la touche est perdue (session retiree, changement de serveur). */
    private static final long STALE_NANOS = (FishFight.ROUND_TIMEOUT + 60) * TICK_NANOS;

    private static boolean active;
    private static long seed;
    private static int rarity, rounds, round, period, hits, misses, allowed, last, status, kind;
    private static double half;
    private static String name = "";
    private static final int[] RESULTS = {-1, -1, -1};
    private static long roundStartNanos, lastChangeNanos, endNanos;

    private FishFightClient() {
    }

    public static boolean active() {
        if (active && status == 0 && System.nanoTime() > roundStartNanos + STALE_NANOS) active = false;
        return active && status == 0;
    }

    public static void receive(CompoundTag t) {
        long nowN = System.nanoTime();
        boolean fresh = !active || status != 0 && t.getIntOr("status", 0) == 0;
        int before = fresh ? 0 : hits + misses;
        if (fresh) java.util.Arrays.fill(RESULTS, -1);
        int newRound = t.getIntOr("round", 0);
        seed = t.getLongOr("seed", 0);
        rarity = t.getIntOr("rarity", 0);
        rounds = t.getIntOr("rounds", 1);
        period = Math.max(2, t.getIntOr("period", 40));
        half = t.getFloatOr("half", 0.15F);
        hits = t.getIntOr("hits", 0);
        misses = t.getIntOr("misses", 0);
        allowed = t.getIntOr("allowed", 0);
        last = t.getIntOr("last", -1);
        status = t.getIntOr("status", 0);
        kind = t.getIntOr("kind", 0);
        name = t.getStringOr("name", "");
        round = newRound;
        int done = hits + misses;
        if (done > before && done <= RESULTS.length && last >= 0) RESULTS[done - 1] = last;
        roundStartNanos = nowN + t.getIntOr("delay", 0) * TICK_NANOS;
        lastChangeNanos = nowN;
        if (!active || status != 0) endNanos = nowN + RESULT_NANOS;
        active = true;
    }

    /** Arrete l'affichage (deconnexion, outils de verification). */
    public static void reset() {
        active = false;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    /** Pendant la touche, un clic droit ne doit pas relancer la ligne : le serveur le prend comme clic sur la jauge. */
    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() || !active() || !(event.getItemStack().getItem() instanceof FishingRodItem)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!active) return;
        long now = System.nanoTime();
        if (status != 0 && now > endNanos || status == 0 && now > roundStartNanos + STALE_NANOS) {
            active = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        int barW = Math.min(220, w - 40), barH = 12;
        int x0 = (w - barW) / 2, y0 = h - 88;
        int color = 0xFF000000 | Rarity.values()[Math.clamp(rarity, 0, Rarity.values().length - 1)].rgb();
        boolean over = status != 0;

        // cadre
        g.fill(x0 - 4, y0 - 22, x0 + barW + 4, y0 + barH + 16, 0xB0101418);
        g.fill(x0 - 4, y0 - 22, x0 + barW + 4, y0 - 21, color);
        g.fill(x0 - 4, y0 + barH + 15, x0 + barW + 4, y0 + barH + 16, color);
        g.fill(x0, y0, x0 + barW, y0 + barH, 0xFF1C2630);
        g.fill(x0, y0, x0 + barW, y0 + 1, 0xFF3A4A5A);

        String title = over ? name : (kind == 2 ? "Un coffre mord à la ligne !" : "Quelque chose mord !");
        if (over && status == 2) title = "La prise s'est échappée";
        int titleColor = over ? (status == 1 ? color : 0xFFFF7777) : 0xFFE8E0D0;
        g.centeredText(mc.font, Component.literal(title), w / 2, y0 - 16, titleColor);

        boolean live = !over && now >= roundStartNanos;
        if (!over) {
            // zone cible
            double c = FishFight.center(seed, round);
            int zx0 = x0 + (int) Math.round((c - half) * barW), zx1 = x0 + (int) Math.round((c + half) * barW);
            zx0 = Math.max(x0, zx0);
            zx1 = Math.min(x0 + barW, zx1);
            int zc0 = x0 + (int) Math.round((c - half * FishFight.PERFECT_FRACTION) * barW);
            int zc1 = x0 + (int) Math.round((c + half * FishFight.PERFECT_FRACTION) * barW);
            if (live || ((now - lastChangeNanos) / TICK_NANOS) % 4 < 3) {
                g.fill(zx0, y0 + 1, zx1, y0 + barH - 1, (color & 0xFFFFFF) | 0x90000000);
                g.fill(zc0, y0 + 1, zc1, y0 + barH - 1, 0xFFFFFFFF & 0x00FFFFFF | 0xC0000000 | (color & 0xFFFFFF));
            }
            if (live) {
                long t = (now - roundStartNanos) / TICK_NANOS;
                double frac = (now - roundStartNanos) / (double) TICK_NANOS - t;
                double pos = FishFight.cursor(t, period, FishFight.phase(seed, round, period)) * (1 - frac)
                        + FishFight.cursor(t + 1, period, FishFight.phase(seed, round, period)) * frac;
                int nx = x0 + (int) Math.round(pos * barW);
                g.fill(nx - 1, y0 - 4, nx + 2, y0 + barH + 4, 0xFFFFFFFF);
                g.fill(nx, y0 - 5, nx + 1, y0 - 4, 0xFFFFFFFF);
                g.centeredText(mc.font, Component.literal("Clic droit !"), w / 2, y0 + barH + 5, 0xFFE8E0D0);
            } else {
                g.centeredText(mc.font, Component.literal("Manche " + (round + 1) + " sur " + rounds + "…"), w / 2, y0 + barH + 5, 0xFF9AA8B8);
            }
        } else {
            g.centeredText(mc.font, Component.literal(status == 1 ? "Prise ramenée !" : "Raté…"), w / 2, y0 + 3, status == 1 ? 0xFF9BE070 : 0xFFFF7777);
        }
        // pastilles de manches : reussie (vert), ratee (rouge), a venir (gris)
        int dot = 6, gap = 4, total = rounds * dot + (rounds - 1) * gap;
        int dx = x0 + barW - total;
        for (int i = 0; i < rounds && i < RESULTS.length; i++) {
            int col = RESULTS[i] == 2 ? 0xFFFFE066 : RESULTS[i] == 1 ? 0xFF9BE070 : RESULTS[i] == 0 ? 0xFFFF5555 : (i == round && !over ? color : 0xFF4A5560);
            g.fill(dx + i * (dot + gap), y0 + barH + 5, dx + i * (dot + gap) + dot, y0 + barH + 5 + dot, col);
        }
        if (last >= 0 && (now - lastChangeNanos) < 700_000_000L) {
            String t = last == 2 ? "Parfait !" : last == 1 ? "Bien !" : "Raté";
            int c = last == 2 ? 0xFFFFE066 : last == 1 ? 0xFF9BE070 : 0xFFFF7777;
            g.centeredText(mc.font, Component.literal(t), w / 2, y0 - 34, c);
        }
        if (allowed > 0 && !over) {
            g.text(mc.font, Component.literal("Erreurs permises : " + Math.max(0, allowed - misses)), x0, y0 + barH + 5, 0xFF7FD0F0, false);
        }
    }
}
