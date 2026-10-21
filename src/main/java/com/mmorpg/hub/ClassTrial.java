package com.mmorpg.hub;

import com.mmorpg.MMORPG;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.LevelSystem;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import com.mmorpg.server.MountManager;
import com.mmorpg.server.PetManager;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.skill.Skill;
import com.mmorpg.skill.Skills;
import com.mojang.serialization.DataResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Essai temporaire d'une classe dans la salle d'epreuve du hub.
 *
 * <p>Au debut de l'essai, tout le personnage est sauvegarde dans {@link HubPlayerData} (donnees RPG completes, sac et
 * equipement, position de retour) ; le joueur recoit alors la classe, le niveau, l'equipement et les competences choisis.
 * A la fin (sortie de la salle, PNJ, deconnexion, changement de dimension ou reconnexion apres un plantage), tout est restaure
 * tel quel. Rien ne peut etre gagne pendant l'essai : ni XP, ni or, ni butin, ni progression de quete, et aucun objet ne peut
 * etre jete ou range hors de l'inventaire temporaire.</p>
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ClassTrial {
    public static final int[] LEVELS = {1, 25, 50, 75, 100};

    private ClassTrial() {
    }

    public static HubPlayerData data(net.minecraft.world.entity.player.Player p) {
        return p.getData(ModAttachments.HUB);
    }

    public static boolean active(net.minecraft.world.entity.player.Player p) {
        return p != null && data(p).inTrial;
    }

    // ================================================================== demarrage

    /** Demarre (ou change) l'essai d'une classe. Renvoie un message d'erreur, ou null en cas de succes. */
    public static @Nullable String start(ServerPlayer p, PlayerClass cls, int level) {
        if (!cls.isPlayable()) return "Classe inconnue.";
        if (java.util.Arrays.stream(LEVELS).noneMatch(l -> l == level)) return "Niveau d'essai inconnu.";
        HubData hub = HubData.get(p.level().getServer());
        ServerLevel hubLevel = hub.level(p.level().getServer());
        if (!hub.exists() || hubLevel == null) return "Le hub n'a pas été construit (/mmorpg hub).";
        if (p.isSpectator()) return "Impossible en mode spectateur.";
        if (CinematicManager.isLocked(p)) return "Patientez, la cinématique est en cours.";
        HubPlayerData hd = data(p);
        PlayerData d = RpgPlayers.get(p);
        boolean fresh = !hd.inTrial;
        if (fresh) {
            if (p.level() != hubLevel) return "Les essais ont lieu au hub d'Eldoria.";
            save(p, hd, d);
        }
        // l'essai remplace temporairement le personnage ; le familier et la monture sont renvoyes
        PetManager.despawn(p);
        MountManager.despawn(p);
        p.stopRiding();
        apply(p, d, cls, level);
        hd.inTrial = true;
        hd.trialClass = cls.ordinal();
        hd.trialLevel = level;
        hd.triedMask |= 1 << cls.ordinal();
        if (fresh) hd.trialStartTick = p.level().getGameTime();
        if (fresh || !HubLayout.roomBounds(hub.origin()).contains(p.position())) {
            Vec3 at = HubLayout.center(hub.origin(), HubLayout.TRIAL_SPAWN);
            p.teleportTo(hubLevel, at.x, at.y, at.z, Set.of(), 180F, 0F, true);
            hubLevel.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1, at.z, 40, 0.4, 0.8, 0.4, 0.2);
            hubLevel.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.3F);
        }
        RpgPlayers.recompute(p);
        RpgPlayers.fullRestore(p);
        RpgPlayers.sync(p);
        sendState(p);
        p.sendOverlayMessage(Component.literal("Essai : " + cls.label + " niveau " + level + " — rien n'est conservé.").withColor(cls.color & 0xFFFFFF));
        return null;
    }

    /** Sauvegarde du personnage avant l'essai. */
    private static void save(ServerPlayer p, HubPlayerData hd, PlayerData d) {
        hd.snapshot = d.save();
        hd.inventory = saveInventory(p);
        hd.returnDim = p.level().dimension().identifier().toString();
        hd.returnX = p.getX();
        hd.returnY = p.getY();
        hd.returnZ = p.getZ();
        hd.returnYaw = p.getYRot();
        hd.returnPitch = p.getXRot();
    }

    /** Donne la classe, le niveau, les competences et l'equipement de l'essai. */
    private static void apply(ServerPlayer p, PlayerData d, PlayerClass cls, int level) {
        d.playerClass = cls;
        d.level = level;
        d.xp = 0;
        d.evolution = PlayerClass.tier(level);
        d.skillRanks.clear();
        java.util.Arrays.fill(d.skillBar, "");
        java.util.Arrays.fill(d.allocated, 0);
        d.buffs.clear();
        d.cooldowns.clear();
        d.activePet = "";
        d.activeMount = "";
        d.unlockSkills();
        // points d'attribut : repartis entre les deux attributs de la classe
        int points = LevelSystem.attributePointsAt(level);
        Stat[] main = cls.mainAttributes();
        for (int i = 0; i < points; i++) d.allocated[main[i % main.length].primaryIndex()]++;
        d.attributePoints = 0;
        // points de competence : rangs repartis entre les competences actives debloquees
        int spare = d.availableSkillPoints();
        boolean any = true;
        while (spare > 0 && any) {
            any = false;
            for (Skill s : Skills.forClass(cls)) {
                if (spare <= 0) break;
                if (d.skillRanks.containsKey(s.id) && d.skillRank(s.id) < 5) {
                    d.skillRanks.merge(s.id, 1, Integer::sum);
                    spare--;
                    any = true;
                }
            }
        }
        d.hp = -1;
        d.mana = -1;
        d.dirty = true;
        // sac et equipement
        Inventory inv = p.getInventory();
        inv.clearContent();
        ItemDefs.WeaponDef weapon = null;
        for (ItemDefs.WeaponDef w : ItemDefs.WEAPONS) {
            if (w.cls() == cls && w.level() <= level && (weapon == null || w.level() >= weapon.level())) weapon = w;
        }
        if (weapon != null) inv.setItem(0, stack(weapon.id(), 1));
        ItemDefs.ArmorSet best = null;
        for (ItemDefs.ArmorSet s : ItemDefs.SETS.values()) {
            if (s.cls() == cls && s.level() <= level && (best == null || s.level() >= best.level())) best = s;
        }
        if (best != null) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < ItemDefs.PIECES.length; i++) {
                ItemStack piece = stack(best.id() + "_" + ItemDefs.PIECES[i], 1);
                if (!piece.isEmpty()) p.setItemSlot(slots[i], piece);
            }
        }
        inv.setItem(1, new ItemStack(ModItems.POTION_SOIN_MINEURE.get(), 5));
        inv.setItem(2, new ItemStack(ModItems.POTION_MANA_MINEURE.get(), 5));
        inv.setSelectedSlot(0);
    }

    private static ItemStack stack(String id, int count) {
        Item item = ModItems.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    // ================================================================== fin / restauration

    /** Termine l'essai et restaure le personnage. {@code teleportBack} : retour a la position d'avant l'essai. */
    public static void end(ServerPlayer p, boolean teleportBack, @Nullable String message) {
        HubPlayerData hd = data(p);
        if (!hd.inTrial) return;
        PlayerData d = RpgPlayers.get(p);
        if (hd.snapshot != null) d.load(hd.snapshot);
        restoreInventory(p, hd.inventory);
        if (teleportBack) teleportToReturn(p, hd);
        hd.inTrial = false;
        hd.snapshot = null;
        hd.inventory = null;
        p.removeAllEffects();
        RpgPlayers.recompute(p);
        RpgPlayers.sync(p);
        RpgPlayers.sendVitals(p, true);
        sendState(p);
        if (message != null) p.sendOverlayMessage(Component.literal(message).withColor(0xA0E0A0));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_OUT, SoundSource.PLAYERS, 0.8F, 1.1F);
    }

    private static void teleportToReturn(ServerPlayer p, HubPlayerData hd) {
        ServerLevel target = null;
        for (ServerLevel l : p.level().getServer().getAllLevels()) {
            if (l.dimension().identifier().toString().equals(hd.returnDim)) target = l;
        }
        if (target == null) target = p.level().getServer().overworld();
        p.teleportTo(target, hd.returnX, hd.returnY, hd.returnZ, Set.of(), hd.returnYaw, hd.returnPitch, true);
    }

    private static ListTag saveInventory(ServerPlayer p) {
        ListTag list = new ListTag();
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            DataResult<Tag> enc = ItemStack.CODEC.encodeStart(ops, s);
            Tag t = enc.result().orElse(null);
            if (t == null) {
                MMORPG.LOGGER.error("[MMORPG] Essai : objet non sauvegardable dans le slot {} ({})", i, enc.error().map(e -> e.message()).orElse("?"));
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putInt("slot", i);
            e.put("item", t);
            list.add(e);
        }
        return list;
    }

    private static void restoreInventory(ServerPlayer p, @Nullable ListTag list) {
        Inventory inv = p.getInventory();
        inv.clearContent();
        if (list == null) return;
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            Tag t = e.get("item");
            if (t == null) continue;
            ItemStack s = ItemStack.CODEC.parse(ops, t).result().orElse(ItemStack.EMPTY);
            int slot = e.getIntOr("slot", -1);
            if (!s.isEmpty() && slot >= 0 && slot < inv.getContainerSize()) inv.setItem(slot, s);
        }
        inv.setSelectedSlot(Math.max(0, Math.min(8, inv.getSelectedSlot())));
    }

    /** Informe le client (indicateur d'essai du HUD). */
    public static void sendState(ServerPlayer p) {
        HubPlayerData hd = data(p);
        CompoundTag t = new CompoundTag();
        t.putBoolean("active", hd.inTrial);
        t.putInt("class", hd.trialClass);
        t.putInt("level", hd.trialLevel);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.TRIAL_STATE, t));
    }

    // ================================================================== evenements

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        HubPlayerData hd = data(p);
        if (hd.inTrial) {
            MMORPG.LOGGER.info("[MMORPG] {} s'est reconnecte pendant un essai de classe : restauration du personnage", p.getGameProfile().name());
            end(p, true, "Votre personnage a été restauré (essai interrompu).");
        }
    }

    /** Deconnexion : le joueur est restaure avant l'ecriture de ses donnees (la sauvegarde persistante couvre aussi un plantage). */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && data(p).inTrial) end(p, true, null);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && data(p).inTrial) end(p, false, "Essai terminé : personnage restauré.");
    }

    /** Controle periodique : sortir de la salle d'epreuve met fin a l'essai. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;
        HubPlayerData hd = data(p);
        if (!hd.inTrial) return;
        HubData hub = HubData.get(p.level().getServer());
        if (!hub.exists() || p.level() != hub.level(p.level().getServer()) || !HubLayout.roomBounds(hub.origin()).contains(p.position())
                || p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            end(p, false, "Essai terminé : personnage restauré.");
        }
    }

    /** Pas de mort pendant un essai (secours : la sante RPG est deja geree dans RpgCombat). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && data(p).inTrial) {
            event.setCanceled(true);
            revive(p);
        }
    }

    /** Appele quand un coup serait mortel : le joueur est soigne et replace au depart de la salle. */
    public static void revive(ServerPlayer p) {
        HubData hub = HubData.get(p.level().getServer());
        ServerLevel level = hub.level(p.level().getServer());
        p.setHealth(p.getMaxHealth());
        RpgPlayers.fullRestore(p);
        if (hub.exists() && level != null) {
            Vec3 at = HubLayout.center(hub.origin(), HubLayout.TRIAL_SPAWN);
            p.teleportTo(level, at.x, at.y, at.z, Set.of(), 180F, 0F, true);
        }
        p.sendOverlayMessage(Component.literal("Vaincu ! L'essai continue, sans aucune perte.").withColor(0xFFC060));
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p && data(p).inTrial) {
            event.setCanceled(true);
            p.sendOverlayMessage(Component.literal("Les objets de l'essai ne peuvent pas être jetés.").withColor(0xFF8080));
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (event.getPlayer() instanceof ServerPlayer p && data(p).inTrial) event.setCanPickup(net.minecraft.util.TriState.FALSE);
    }

    /** Aucun coffre ni conteneur pendant un essai (rien ne doit sortir de l'inventaire temporaire). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !data(p).inTrial) return;
        var be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof net.minecraft.world.MenuProvider || be instanceof net.minecraft.world.Container
                || event.getLevel().getBlockState(event.getPos()).getBlock() instanceof com.mmorpg.block.ForgeBlock) {
            event.setCanceled(true);
        }
    }
}
