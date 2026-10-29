package com.mmorpg.fishing;

import com.mmorpg.MMORPG;
import com.mmorpg.item.AeloriaRodItem;
import com.mmorpg.job.Job;
import com.mmorpg.job.JobSystem;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Peche d'Aeloria : remplace le butin vanilla par la table de FishingRules, joue le mini-jeu de la touche (clic droit dans la zone) et
 * distribue experience, journal de peche, doubles prises, prises rares et ingredients marins du metier de pecheur.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class FishingEvents {
    private FishingEvents() {
    }

    /** Touche en cours de combat : la prise tiree, son contexte et le mini-jeu. */
    public static final class Session {
        public final FishCatalog.Entry entry;
        public final FishingRules.Context ctx;
        public final FishFight fight;
        public final Random rnd;
        public int lastResult = -1;

        Session(FishCatalog.Entry entry, FishingRules.Context ctx, FishFight fight, Random rnd) {
            this.entry = entry;
            this.ctx = ctx;
            this.fight = fight;
            this.rnd = rnd;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static Session session(ServerPlayer p) {
        return SESSIONS.get(p.getUUID());
    }

    // ------------------------------------------------------------------ rang de canne

    /** Rang de la canne tenue : 1 a 6 pour les cannes d'Aeloria, 0 pour la canne vanilla. */
    public static int rodTier(ItemStack stack) {
        return stack.getItem() instanceof AeloriaRodItem r ? r.rod().tier() : 0;
    }

    public static double rodZone(int tier) {
        FishCatalog.Rod r = FishCatalog.rod(tier);
        return r == null ? 0.9 : r.zone();
    }

    private static ItemStack heldRod(ServerPlayer p) {
        ItemStack main = p.getMainHandItem();
        if (main.getItem() instanceof FishingRodItem) return main;
        ItemStack off = p.getOffhandItem();
        return off.getItem() instanceof FishingRodItem ? off : main;
    }

    // ------------------------------------------------------------------ touche

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void fished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        if (!com.mmorpg.server.FeatureFlags.enabled(p.level().getServer(), "peche")) return;
        FishingHook hook = event.getHookEntity();
        // la prise est geree ici : rien ne tombe de la table vanilla, la canne use toujours un point
        event.setCanceled(true);
        event.damageRodBy(1);
        if (SESSIONS.containsKey(p.getUUID())) return;
        ItemStack rod = heldRod(p);
        int jobLevel = JobSystem.level(p, Job.PECHEUR);
        int tier = rodTier(rod);
        var pos = hook.blockPosition();
        FishingRules.Context ctx = new FishingRules.Context(jobLevel, tier, FishingWorld.habitat(level, pos), FishingWorld.night(level),
                FishingWorld.rain(level, pos));
        Random rnd = new Random(p.getRandom().nextLong());
        FishCatalog.Entry entry = FishingRules.roll(ctx, rnd);
        start(p, ctx, entry, rnd, tier);
    }

    /** Demarre la touche pour une prise donnee (un dechet est remonte tout de suite, sans mini-jeu). */
    public static Session start(ServerPlayer p, FishingRules.Context ctx, FishCatalog.Entry entry, Random rnd, int tier) {
        if (entry.kind() == FishCatalog.Kind.DECHET) {
            award(p, entry, ctx, false, rnd);
            return null;
        }
        double zone = rodZone(tier) * FishingRules.jobZoneBonus(ctx.jobLevel());
        long now = p.level().getServer().getTickCount();
        FishFight fight = new FishFight(entry.rarity(), tier, zone, rnd.nextLong(), now);
        Session s = new Session(entry, ctx, fight, rnd);
        SESSIONS.put(p.getUUID(), s);
        p.level().playSound(null, p.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.8F, 0.9F);
        p.sendOverlayMessage(Component.literal("Ça mord ! Clic droit quand l'aiguille est dans la zone").withColor(entry.rarity().rgb()));
        send(p, s, 0);
        return s;
    }

    /** Clic droit pendant une touche : valide l'aiguille. Bloque aussi le lancer d'une canne trop avancee pour le niveau. */
    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof FishingRodItem)) return;
        Session s = SESSIONS.get(player.getUUID());
        if (s != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getHand() == InteractionHand.MAIN_HAND || !(player.getMainHandItem().getItem() instanceof FishingRodItem)) pressed(player, s);
            return;
        }
        if (player.fishing == null && stack.getItem() instanceof AeloriaRodItem r) {
            int lvl = JobSystem.level(player, Job.PECHEUR);
            if (lvl < r.rod().minJobLevel()) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                player.sendOverlayMessage(Component.literal("Il faut être pêcheur niveau " + r.rod().minJobLevel() + " pour utiliser cette canne").withColor(0xFFFF5555));
            }
        }
    }

    private static void pressed(ServerPlayer p, Session s) {
        press(p, s, p.level().getServer().getTickCount(), (int) Math.round(p.connection.latency() / 50.0));
    }

    /** Clic du joueur au tick {@code now} (point d'entree commun, aussi utilise par les verifications). */
    public static FishFight.Press press(ServerPlayer p, Session s, long now, int lag) {
        FishFight.Press result = s.fight.press(now, lag);
        if (result == null) return null;
        s.lastResult = result.ordinal();
        float pitch = result == FishFight.Press.PERFECT ? 1.7F : result == FishFight.Press.HIT ? 1.3F : 0.6F;
        p.level().playSound(null, p.blockPosition(), result == FishFight.Press.MISS ? SoundEvents.FISHING_BOBBER_RETRIEVE : SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.7F, pitch);
        step(p, s);
        return result;
    }

    /** Apres un clic ou un delai depasse : termine la touche ou envoie l'etat de la manche suivante. */
    private static void step(ServerPlayer p, Session s) {
        if (s.fight.finished()) {
            SESSIONS.remove(p.getUUID());
            finish(p, s);
        } else {
            send(p, s, 0);
        }
    }

    private static void finish(ServerPlayer p, Session s) {
        if (s.fight.success()) {
            send(p, s, 1);
            award(p, s.entry, s.ctx, s.fight.perfect(), s.rnd);
        } else {
            send(p, s, 2);
            p.sendOverlayMessage(Component.literal("Ça s'échappe… la prise vous glisse entre les doigts").withColor(0xFFFF7777));
            p.level().playSound(null, p.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8F, 0.5F);
            JobSystem.addXp(p, Job.PECHEUR, FishCatalog.ESCAPE_XP);
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) return;
        long now = event.getServer().getTickCount();
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) {
                it.remove();
                continue;
            }
            Session s = e.getValue();
            if (s.fight.tick(now)) {
                s.lastResult = 0;
                p.level().playSound(null, p.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.7F, 0.6F);
                if (s.fight.finished()) {
                    it.remove();
                    finish(p, s);
                } else {
                    send(p, s, 0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    /** Etat envoye au client : status 0 = en cours, 1 = prise ramenee, 2 = prise perdue. */
    private static void send(ServerPlayer p, Session s, int status) {
        long now = p.level().getServer().getTickCount();
        FishFight f = s.fight;
        CompoundTag t = new CompoundTag();
        t.putLong("seed", f.seed);
        t.putInt("rarity", s.entry.rarity().ordinal());
        t.putInt("rounds", f.rounds);
        t.putInt("round", Math.min(f.round(), f.rounds - 1));
        t.putInt("period", f.period);
        t.putFloat("half", (float) f.halfWidth);
        t.putInt("hits", f.hits());
        t.putInt("misses", f.misses());
        t.putInt("allowed", f.allowedMisses);
        t.putInt("delay", (int) Math.max(0, f.roundStart() - now));
        t.putInt("last", s.lastResult);
        t.putInt("status", status);
        t.putInt("kind", s.entry.kind().ordinal());
        t.putString("name", status == 0 ? "" : Component.translatable("item.mmorpg." + s.entry.id()).getString());
        Net.toPlayer(p, new Payloads.FishFightState(t));
    }

    // ------------------------------------------------------------------ recompenses

    /** Donne la prise, l'experience et les bonus du metier. */
    public static void award(ServerPlayer p, FishCatalog.Entry entry, FishingRules.Context ctx, boolean perfect, Random rnd) {
        PlayerData d = RpgPlayers.get(p);
        int jobLevel = ctx.jobLevel();
        Item item = ModItems.get(entry.id());
        int count = entry.kind() != FishCatalog.Kind.TRESOR && JobSystem.rollDouble(p, Job.PECHEUR, p.getRandom()) ? 2 : 1;
        int size = FishingRules.sizeCm(entry, perfect, rnd);
        if (item != null) JobSystem.give(p, new ItemStack(item, count));
        d.fishCaught.merge(entry.id(), count, Integer::sum);
        boolean record = size > 0 && size > d.fishBest.getOrDefault(entry.id(), 0);
        if (record) d.fishBest.put(entry.id(), size);
        d.dirty = true;
        JobSystem.addXp(p, Job.PECHEUR, entry.kind() == FishCatalog.Kind.DECHET ? FishCatalog.JUNK_XP : FishingRules.xp(entry, perfect, jobLevel));

        Component name = new ItemStack(item == null ? Items.COD : item).getHoverName();
        Component line = Component.literal(count > 1 ? "Prise doublée ! " + count + " × " : "").withColor(0xFF4FB3E8).append(name);
        if (size > 0) line = line.copy().append(Component.literal(" · " + size + " cm").withColor(0xFFB9ABCF));
        if (perfect) line = line.copy().append(Component.literal(" ★").withColor(0xFFFFE066));
        p.sendOverlayMessage(line);
        if (entry.rarity().ordinal() >= Rarity.RARE.ordinal() && entry.kind() != FishCatalog.Kind.DECHET || record && d.fishCaught.getOrDefault(entry.id(), 0) > count) {
            p.sendSystemMessage(Component.literal("🎣 ").withColor(entry.rarity().rgb()).append(name.copy())
                    .append(Component.literal(size > 0 ? " — " + size + " cm" + (record ? " (nouveau record !)" : "") : "").withColor(0xFFB9ABCF)));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.35F, 1.2F + entry.rarity().ordinal() * 0.1F);
        if (entry.rarity().ordinal() >= Rarity.LEGENDAIRE.ordinal() && entry.kind() != FishCatalog.Kind.DECHET) {
            p.level().getServer().getPlayerList().broadcastSystemMessage(Component.literal("🎣 ").withColor(entry.rarity().rgb())
                    .append(p.getDisplayName().copy().withColor(0xFFFFFFFF)).append(Component.literal(" a pêché ").withColor(0xFFB9ABCF))
                    .append(name.copy()).append(Component.literal(size > 0 ? " (" + size + " cm) !" : " !").withColor(0xFFB9ABCF)), false);
        }
        com.mmorpg.achievement.Achievements.add(p, entry.kind() == FishCatalog.Kind.TRESOR ? com.mmorpg.achievement.Achievement.TREASURES_FISHED
                : com.mmorpg.achievement.Achievement.FISH_CAUGHT, count);

        if (entry.kind() == FishCatalog.Kind.DECHET) return;
        // prises bonus du metier : poisson rare, ingredient marin (sans experience en plus)
        if (rnd.nextDouble() < Job.rareCatchChance(jobLevel)) {
            FishCatalog.Entry bonus = FishingRules.rollRare(ctx, rnd);
            Item bi = ModItems.get(bonus.id());
            if (bi != null) {
                JobSystem.give(p, new ItemStack(bi));
                d.fishCaught.merge(bonus.id(), 1, Integer::sum);
                int bsize = FishingRules.sizeCm(bonus, false, rnd);
                if (bsize > d.fishBest.getOrDefault(bonus.id(), 0)) d.fishBest.put(bonus.id(), bsize);
                p.sendSystemMessage(Component.literal("★ Prise rare : ").withColor(0xFFE2B85A).append(new ItemStack(bi).getHoverName()));
            }
        }
        if (rnd.nextDouble() < Job.ingredientChance(jobLevel)) {
            FishCatalog.Ingredient ing = FishingRules.rollIngredient(jobLevel, rnd);
            Item ii = ModItems.get(ing.id());
            if (ii != null) {
                int n = 1 + rnd.nextInt(Job.tier(jobLevel) + 1);
                JobSystem.give(p, new ItemStack(ii, n));
                p.sendOverlayMessage(Component.literal("Ingrédient marin : " + n + " × ").withColor(0xFF7FD0F0).append(new ItemStack(ii).getHoverName()));
            }
        }
    }
}
