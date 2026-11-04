package com.mmorpg.character;

import com.mmorpg.MMORPG;
import com.mmorpg.hub.ClassTrial;
import com.mmorpg.network.NetworkConfig;
import com.mmorpg.network.NetworkPlayers;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.FeatureFlags;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.store.StoreLedger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Selection de personnage a la connexion : jusqu'a 3 emplacements gratuits, 12 avec des credits de boutique.
 * Chaque personnage a ses propres donnees (RPG, inventaire, coffre de l'Ender, banque, guilde...). Le personnage joue vit dans les
 * donnees du joueur ; les autres sont rangees dans {@link CharacterStore}. Interrupteur : fonctionnalite « personnages ».
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Characters {
    private Characters() {
    }

    private static final Set<UUID> SELECTING = new HashSet<>();
    private static final java.util.Map<UUID, UUID> BUY_REQUEST = new java.util.HashMap<>();
    private static final EquipmentSlot[] SHOWN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    public static boolean selecting(ServerPlayer p) {
        return SELECTING.contains(p.getUUID());
    }

    // ------------------------------------------------------------------ connexion

    /** Appele a la connexion, apres le chargement du profil et avant l'initialisation RPG : ouvre l'ecran et differe la suite. */
    public static boolean intercept(ServerPlayer p) {
        try {
            if (!FeatureFlags.enabled(p.level().getServer(), "personnages")) return false;
            if (NetworkPlayers.isArriving(p)) return false;                     // simple changement de serveur du reseau
            if (ClassTrial.data(p).inTrial) ClassTrial.end(p, true, null);
            SELECTING.add(p.getUUID());
            open(p, "");
            return true;
        } catch (Exception e) {
            MMORPG.LOGGER.error("[Aeloria] Selection de personnage indisponible : connexion normale", e);
            SELECTING.remove(p.getUUID());
            return false;
        }
    }

    /** Le chat et le tab affichent le nom du personnage actif, pas le pseudo du compte. */
    public static void refreshNames(ServerPlayer p) {
        p.refreshDisplayName();
        p.refreshTabListName();
    }

    /** Renomme le personnage d'un compte (emplacement 0-11). Commande admin. */
    public static void rename(ServerPlayer target, int slot, String name) throws Exception {
        require(name.matches("[\\p{L}0-9 '_\\-\\[\\]().]{3,16}"), "Nom : 3 à 16 caractères (lettres, chiffres, espaces, [ ] ( ) . - _).");
        var server = target.level().getServer();
        var store = CharacterStore.load(server, target.getUUID());
        PlayerData d = RpgPlayers.get(target);
        for (var other : store.slots.values()) require(other.slot == slot || !other.name.equalsIgnoreCase(name), "Ce compte a déjà un personnage de ce nom.");
        var s = store.slots.get(slot);
        require(s != null || slot == d.charSlot, "Aucun personnage à cet emplacement.");
        if (s != null) { s.name = name; store.write(); }
        if (slot == d.charSlot) {
            d.charName = name;
            d.dirty = true;
            refreshNames(target);
            savedNow(target);
        }
    }

    private static void finish(ServerPlayer p) {
        SELECTING.remove(p.getUUID());
        refreshNames(p);
        if (NetworkConfig.current.enabled && NetworkConfig.current.role == NetworkConfig.Role.LOBBY) {
            // serveur de connexion : le personnage est choisi, on l'envoie sur le serveur de jeu (Spawn s'il a une classe, sinon hub)
            boolean playable = RpgPlayers.get(p).playerClass.isPlayable();
            NetworkPlayers.route(p, playable ? NetworkConfig.Role.MAIN : NetworkConfig.Role.HUB, playable ? "spawn" : "hub");
            return;
        }
        com.mmorpg.dungeon.DungeonLobby.arriveIfNeeded(p);
        RpgPlayers.onLogin(p);
        refreshNames(p);
    }

    @SubscribeEvent
    public static void damage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) {
        if (!SELECTING.isEmpty() && e.getEntity() instanceof ServerPlayer p && SELECTING.contains(p.getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Pre e) {
        if (e.getEntity() instanceof ServerPlayer p && !SELECTING.isEmpty() && SELECTING.contains(p.getUUID())) {
            p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
        }
    }

    public static void logout(ServerPlayer p) {
        boolean was = SELECTING.remove(p.getUUID());
        BUY_REQUEST.remove(p.getUUID());
        if (!was) try {
            PlayerData d = RpgPlayers.get(p);
            if (d.charSlot >= 0 && FeatureFlags.enabled(p.level().getServer(), "personnages")) {
                var store = CharacterStore.load(p.level().getServer(), p.getUUID());
                if (store.slots.containsKey(d.charSlot)) { refresh(p, store.slots.get(d.charSlot)); store.write(); }
            }
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------ resume

    private static String place(ServerPlayer p) {
        String path = p.level().dimension().identifier().getPath();
        String role = NetworkConfig.current.enabled ? NetworkConfig.current.role.name() : "";
        if (path.contains("hub")) return "Citadelle d'Aeloria";
        if (path.contains("spawn")) return "Spawn d'Aeloria";
        if (path.contains("mine")) return "Mines d'Aeloria";
        if (path.contains("plain")) return "Plaines d'Aeloria";
        if (path.contains("guild")) return "Hall de guilde";
        if (path.contains("dungeon") || path.contains("donjon") || path.contains("instance")) return "Donjon";
        if (path.contains("competition")) return "Arène";
        return role.equals("MINES") ? "Mines d'Aeloria" : role.equals("PLAINS") ? "Plaines d'Aeloria" : "Aeloria";
    }

    private static CompoundTag equipment(ServerPlayer p) {
        var t = new CompoundTag();
        var ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
        for (EquipmentSlot s : SHOWN) {
            ItemStack item = p.getItemBySlot(s);
            if (!item.isEmpty()) ItemStack.CODEC.encodeStart(ops, item).result().ifPresent(tag -> t.put(s.getName(), tag));
        }
        return t;
    }

    private static void refresh(ServerPlayer p, CharacterStore.Slot s) {
        PlayerData d = RpgPlayers.get(p);
        s.name = d.charName == null || d.charName.isBlank() ? p.getName().getString() : d.charName;
        s.cls = d.playerClass.ordinal();
        s.level = d.level;
        s.place = place(p);
        s.role = NetworkConfig.current.enabled ? NetworkConfig.current.role.name() : "";
        s.dim = p.level().dimension().identifier().toString();
        s.x = p.getX(); s.y = p.getY(); s.z = p.getZ();
        s.saved = System.currentTimeMillis();
        s.equip = equipment(p);
        s.snapshot = null;
    }

    public static int ownedSlots(MinecraftServer server, UUID account) {
        return StoreLedger.BASE_SLOTS + StoreLedger.extraSlots(server, account);
    }

    /** Envoie (ou renvoie) l'ecran avec l'etat a jour. */
    public static void open(ServerPlayer p, String status) throws java.io.IOException {
        var server = p.level().getServer();
        PlayerData d = RpgPlayers.get(p);
        var store = CharacterStore.load(server, p.getUUID());
        boolean changed = false;
        if (d.charSlot >= 0) {
            var live = store.slots.computeIfAbsent(d.charSlot, k -> { var s = new CharacterStore.Slot(); s.slot = k; return s; });
            refresh(p, live);
            changed = true;
        }
        if (changed) store.write();
        int owned = ownedSlots(server, p.getUUID());
        var t = new CompoundTag();
        t.putString("status", status);
        t.putBoolean("selecting", selecting(p));
        t.putInt("owned", owned);
        t.putInt("max", StoreLedger.MAX_SLOTS);
        t.putLong("price", StoreLedger.slotPrice(server));
        t.putLong("credits", StoreLedger.read(server, s -> StoreLedger.account(s, p.getUUID()).credits));
        t.putInt("live", d.charSlot);
        t.putString("account", p.getName().getString());
        var list = new ListTag();
        for (int i = 0; i < owned; i++) {
            var e = new CompoundTag();
            var s = store.slots.get(i);
            e.putInt("slot", i);
            e.putBoolean("empty", s == null);
            if (s != null) {
                e.putString("name", s.name); e.putInt("cls", s.cls); e.putInt("level", s.level);
                e.putString("place", s.place); e.put("equip", s.equip.copy());
            }
            list.add(e);
        }
        t.put("slots", list);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.CHARACTERS, t));
    }

    // ------------------------------------------------------------------ actions

    public static void action(ServerPlayer p, CompoundTag data) {
        if (!selecting(p)) return;                         // phase 1 : uniquement a la connexion
        String action = data.getStringOr("action", "");
        try {
            var server = p.level().getServer();
            int slot = data.getIntOr("slot", -1);
            switch (action) {
                case "open" -> open(p, "");
                case "play" -> {
                    play(p, slot);
                    finish(p);
                }
                case "create" -> {
                    create(p, slot, data.getStringOr("name", "").trim());
                    finish(p);
                }
                case "delete" -> {
                    delete(p, slot, data.getStringOr("confirm", "").trim());
                    open(p, "Personnage supprimé.");
                }
                case "buy" -> {
                    UUID req = BUY_REQUEST.computeIfAbsent(p.getUUID(), k -> UUID.randomUUID());
                    StoreLedger.buySlot(server, p.getUUID(), req, data.getLongOr("price", -1));
                    BUY_REQUEST.remove(p.getUUID());
                    open(p, "Nouvel emplacement débloqué !");
                }
                default -> { }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            reply(p, e.getMessage() == null ? "Action refusée." : e.getMessage());
        } catch (Exception e) {
            MMORPG.LOGGER.error("[Aeloria] Selection de personnage : {}", action, e);
            reply(p, "Action impossible pour le moment. Réessaie.");
        }
    }

    private static void reply(ServerPlayer p, String status) {
        try { open(p, status); } catch (Exception e) { p.sendSystemMessage(Component.literal(status).withColor(0xFF8888)); }
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException(message);
    }

    /** Range le personnage joue dans la fiche (sauvegarde complete) avant de le remplacer. */
    private static void stash(ServerPlayer p, CharacterStore store) throws java.io.IOException {
        PlayerData d = RpgPlayers.get(p);
        if (d.charSlot < 0) return;
        var s = store.slots.computeIfAbsent(d.charSlot, k -> { var n = new CharacterStore.Slot(); n.slot = k; return n; });
        refresh(p, s);
        s.snapshot = NetworkPlayers.snapshot(p);
        store.write();
    }

    private static void play(ServerPlayer p, int slot) throws Exception {
        var server = p.level().getServer();
        require(slot >= 0 && slot < ownedSlots(server, p.getUUID()), "Emplacement indisponible.");
        PlayerData d = RpgPlayers.get(p);
        if (slot == d.charSlot) return;                      // le personnage deja charge : on joue tel quel
        var store = CharacterStore.load(server, p.getUUID());
        var target = store.slots.get(slot);
        require(target != null && target.snapshot != null, "Ce personnage est vide.");
        stash(p, store);
        com.mmorpg.server.MountManager.despawn(p);
        com.mmorpg.server.PetManager.despawn(p);
        NetworkPlayers.restore(p, target.snapshot);
        PlayerData now = RpgPlayers.get(p);
        now.charSlot = slot;
        now.dirty = true;
        target.snapshot = null;
        store.write();
        savedNow(p);
        sendToHome(p, now);
    }

    private static void create(ServerPlayer p, int slot, String name) throws Exception {
        var server = p.level().getServer();
        require(slot >= 0 && slot < ownedSlots(server, p.getUUID()), "Emplacement indisponible.");
        require(name.matches("[\\p{L}0-9 '_\\-\\[\\]().]{3,16}"), "Nom : 3 à 16 caractères (lettres, chiffres, espaces, [ ] ( ) . - _).");
        var store = CharacterStore.load(server, p.getUUID());
        require(!store.slots.containsKey(slot), "Emplacement déjà utilisé.");
        for (var other : store.slots.values()) require(!other.name.equalsIgnoreCase(name), "Tu as déjà un personnage de ce nom.");
        stash(p, store);
        com.mmorpg.server.MountManager.despawn(p);
        com.mmorpg.server.PetManager.despawn(p);
        blank(p, slot, name);
        var s = new CharacterStore.Slot();
        s.slot = slot;
        refresh(p, s);
        store.slots.put(slot, s);
        store.write();
        savedNow(p);
        PlayerData now = RpgPlayers.get(p);
        if (!NetworkConfig.town() && NetworkConfig.current.role != NetworkConfig.Role.LOBBY) sendToHome(p, now);
    }

    /** Remet le joueur a l'etat d'un tout nouveau personnage (comme une premiere connexion). */
    private static void blank(ServerPlayer p, int slot, String name) throws Exception {
        var ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
        var bank = com.mmorpg.economy.BankData.Account.CODEC.parse(ops, new CompoundTag()).getOrThrow();
        var data = new PlayerData();
        data.charSlot = slot;
        data.charName = name;
        p.getInventory().clearContent();
        p.getEnderChestInventory().clearContent();
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND}) p.setItemSlot(s, ItemStack.EMPTY);
        p.containerMenu.setCarried(ItemStack.EMPTY);
        p.removeAllEffects();
        p.setExperienceLevels(0);
        p.setExperiencePoints(0);
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.setData(ModAttachments.PLAYER, data);
        com.mmorpg.network.DungeonTravel.restore(p, null);
        com.mmorpg.economy.BankData.get(p.level().getServer()).replaceAccount(p.getUUID(), bank);
        var hd = ClassTrial.data(p);
        hd.inTrial = false; hd.snapshot = null; hd.inventory = null; hd.cinematicSeen = false; hd.triedMask = 0; hd.tutorialMask = 0; hd.hub = 0;
        p.inventoryMenu.broadcastChanges();
    }

    private static void delete(ServerPlayer p, int slot, String confirm) throws Exception {
        var server = p.level().getServer();
        var store = CharacterStore.load(server, p.getUUID());
        var s = store.slots.get(slot);
        require(s != null, "Emplacement vide.");
        require(confirm.equalsIgnoreCase(s.name), "Écris le nom exact du personnage pour confirmer.");
        PlayerData d = RpgPlayers.get(p);
        UUID key = CharKey.key(p.getUUID(), slot);
        com.mmorpg.guild.GuildService.leaveForDelete(server, key);   // refuse si chef d'une guilde qui a d'autres membres
        store.slots.remove(slot);
        store.write();
        if (slot == d.charSlot) {                                   // personnage joue : on repart d'un etat vide, sans personnage actif
            com.mmorpg.server.MountManager.despawn(p);
            com.mmorpg.server.PetManager.despawn(p);
            blank(p, -1, "");
            savedNow(p);
        }
    }

    private static void savedNow(ServerPlayer p) {
        if (NetworkConfig.current.enabled) NetworkPlayers.save(p);
        else p.level().getServer().getPlayerList().saveAll();
    }

    /** Un personnage change n'est pas la ou il etait : retour au Spawn (ou au hub s'il n'a pas de classe). */
    private static void sendToHome(ServerPlayer p, PlayerData d) {
        if (NetworkConfig.current.enabled && NetworkConfig.current.role == NetworkConfig.Role.LOBBY) return;   // le lobby envoie ensuite vers le serveur de jeu
        if (d.playerClass.isPlayable()) com.mmorpg.hub.HubManager.goToSpawn(p);
        else com.mmorpg.hub.HubManager.goToHub(p);
    }
}
