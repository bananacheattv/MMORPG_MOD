package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.world.AdventurePortalData;
import com.mmorpg.world.AdventurePortalData.Portal;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;

/** Operator-placed portals and a bounded, conflict-aware undo history. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class AdventurePortals {
    private static final Map<UUID, UUID> INSIDE = new HashMap<>();
    private static final Map<UUID, Deque<Change>> HISTORY = new HashMap<>();
    private static final int MAX_HISTORY = 20;
    private record Change(Portal portal, boolean placed, Map<BlockPos, BlockState> before, Map<BlockPos, BlockState> after, long order) {
        Change(Portal portal, boolean placed, Map<BlockPos, BlockState> before, Map<BlockPos, BlockState> after) {
            this(portal, placed, before, after, SpawnEditor.nextOrder());
        }
    }
    private AdventurePortals() { }

    public static AABB passage(Portal portal) {
        BlockPos p = portal.pos();
        return portal.widthX() ? new AABB(p.getX() - 1, p.getY(), p.getZ() + .15, p.getX() + 2, p.getY() + 4, p.getZ() + .85)
                : new AABB(p.getX() + .15, p.getY(), p.getZ() - 1, p.getX() + .85, p.getY() + 4, p.getZ() + 2);
    }
    public static String label(Portal portal) {
        return switch (portal.kind()) {
            case "mine" -> portal.region() < 0 ? "Mines : choix de la région" : com.mmorpg.mine.MineLayout.STYLES.get(portal.region()).name();
            case "plaines" -> portal.region() < 0 ? "Plaines : choix de la région" : com.mmorpg.plains.PlainsLayout.STYLES.get(portal.region()).name();
            case "donjon" -> DungeonType.byId(portal.dungeon()).label + " · " + new String[]{"normal", "héroïque", "mythique"}[portal.difficulty()];
            case "hub" -> "Hub : choix de classe";
            case "spawn" -> "Spawn d'Aeloria";
            default -> "Portail inconnu";
        };
    }
    public static boolean valid(Portal p) {
        return switch (p.kind()) {
            case "mine" -> p.region() >= -1 && p.region() < com.mmorpg.mine.MineLayout.SECTORS;
            case "plaines" -> p.region() >= -1 && p.region() < com.mmorpg.plains.PlainsLayout.REGIONS;
            case "donjon" -> DungeonType.byId(p.dungeon()) != null && p.difficulty() >= 0 && p.difficulty() <= 2;
            case "hub", "spawn" -> true;
            default -> false;
        };
    }
    private static BlockState frame(Portal p, int x, int y) {
        if (Math.abs(x) == 2 && y == 3) return com.mmorpg.registry.DecorBlocks.get(switch (p.kind()) {
            case "mine" -> "cristal_ambre"; case "plaines" -> "cristal_azur"; default -> "cristal_arcane";
        }).defaultBlockState();
        return com.mmorpg.registry.DecorBlocks.get(y == 4 ? "marbre_dore" : "marbre_poli").defaultBlockState();
    }
    private static Map<BlockPos, BlockState> frame(Portal p) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (int x = -2; x <= 2; x++) for (int y = 0; y <= 4; y++) {
            if (Math.abs(x) == 2 || y == 4) blocks.put(p.at(x, y), frame(p, x, y));
        }
        return blocks;
    }
    private static void remember(ServerPlayer p, Change change) {
        Deque<Change> history = HISTORY.computeIfAbsent(p.getUUID(), id -> new ArrayDeque<>());
        history.push(change);
        while (history.size() > MAX_HISTORY) history.removeLast();
    }
    /** Only air is replaced. Terrain, block entities and inventories cannot be overwritten by placement. */
    public static boolean place(ServerPlayer player, Portal portal) {
        ServerLevel level = player.level();
        if (!valid(portal)) return fail(player, "Destination de portail inconnue : utilisez les suggestions de la commande.");
        if (!portal.dimension().equals(level.dimension())) return false;
        var data = AdventurePortalData.get(level.getServer());
        if (data.all().size() >= 256 || data.find(portal.id()) != null) return fail(player, "Limite de portails atteinte ou identifiant déjà utilisé.");
        for (Portal other : data.all()) if (other.dimension().equals(portal.dimension()) && passage(other).inflate(2).intersects(passage(portal))) return fail(player, "Un portail est déjà trop proche.");
        for (int x = -2; x <= 2; x++) for (int y = 0; y <= 4; y++) {
            BlockPos at = portal.at(x, y);
            if (level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at) || !level.hasChunkAt(at) || !level.getBlockState(at).isAir()) return fail(player, "Il faut un espace vide de 5 blocs de large et 5 de haut, dans la bordure du monde. Aucun terrain n'a été remplacé.");
        }
        for (int x = -2; x <= 2; x++) {
            BlockPos ground = portal.at(x, -1);
            if (level.getBlockState(ground).getCollisionShape(level, ground).isEmpty()) return fail(player, "Placez le portail sur un sol plat et solide.");
        }
        var after = frame(portal);
        Map<BlockPos, BlockState> before = new LinkedHashMap<>();
        after.forEach((pos, state) -> before.put(pos, level.getBlockState(pos)));
        after.forEach(level::setBlockAndUpdate);
        data.put(portal);
        remember(player, new Change(portal, true, before, after));
        return true;
    }
    public static boolean remove(ServerPlayer player, Portal portal) {
        if (!portal.dimension().equals(player.level().dimension())) return false;
        var level = player.level();
        var data = AdventurePortalData.get(level.getServer());
        if (!portal.equals(data.find(portal.id()))) return false;
        Map<BlockPos, BlockState> before = new LinkedHashMap<>(), after = new LinkedHashMap<>();
        frame(portal).forEach((pos, state) -> {
            // Keep blocks subsequently changed by a builder.
            if (level.getBlockState(pos).equals(state) && level.getBlockEntity(pos) == null) {
                before.put(pos, state); after.put(pos, Blocks.AIR.defaultBlockState());
            }
        });
        after.forEach(level::setBlockAndUpdate);
        data.remove(portal.id());
        remember(player, new Change(portal, false, before, after));
        return true;
    }
    public static boolean undo(ServerPlayer player) {
        Deque<Change> history = HISTORY.get(player.getUUID());
        if (SpawnEditor.lastOrder(player) > (history == null || history.isEmpty() ? 0 : history.peek().order())) return SpawnEditor.undoOwn(player);
        if (history == null || history.isEmpty()) return fail(player, "Aucune modification de portail à annuler dans cette session.");
        Change change = history.peek();
        var server = player.level().getServer();
        ServerLevel level = server.getLevel(change.portal().dimension());
        if (level == null) return fail(player, "La dimension de cette modification n'est pas disponible.");
        var data = AdventurePortalData.get(server);
        Portal current = data.find(change.portal().id());
        if (change.placed() ? !change.portal().equals(current) : current != null) return fail(player, "Le portail a été modifié depuis : annulation refusée pour préserver ces changements.");
        for (var entry : change.after().entrySet()) {
            if (!level.getBlockState(entry.getKey()).equals(entry.getValue()) || level.getBlockEntity(entry.getKey()) != null) return fail(player, "Un bloc a été modifié depuis : annulation refusée pour ne pas écraser le travail d'un autre bâtisseur.");
        }
        change.before().forEach(level::setBlockAndUpdate);
        if (change.placed()) data.remove(change.portal().id()); else data.put(change.portal());
        history.pop();
        INSIDE.entrySet().removeIf(entry -> entry.getValue().equals(change.portal().id()));
        return true;
    }
    private static boolean fail(ServerPlayer p, String message) { p.sendSystemMessage(Component.literal(message).withColor(0xFF8080)); return false; }

    public static boolean allowed(ServerPlayer p, Portal portal) {
        if (valid(portal) && (portal.kind().equals("hub") || portal.kind().equals("spawn")))
            return p.isAlive() && !com.mmorpg.hub.ClassTrial.active(p) && !com.mmorpg.hub.CinematicManager.isLocked(p)
                    && RpgPlayers.get(p).pvpUntilTick <= p.level().getServer().getTickCount();
        if (!valid(portal) || !p.isAlive() || p.isSpectator() || !RpgPlayers.get(p).playerClass.isPlayable()) return false;
        if (com.mmorpg.hub.ClassTrial.active(p) || com.mmorpg.hub.CinematicManager.isLocked(p)) return false;
        if (RpgPlayers.get(p).pvpUntilTick > p.level().getServer().getTickCount()) return false;
        return switch (portal.kind()) {
            case "mine" -> portal.region() < 0 || (portal.region() < RpgPlayers.get(p).minesUnlocked && RpgPlayers.get(p).level >= com.mmorpg.mine.MineLayout.STYLES.get(portal.region()).minLevel());
            case "plaines" -> portal.region() < 0 || (portal.region() < RpgPlayers.get(p).plainsUnlocked && RpgPlayers.get(p).level >= com.mmorpg.plains.PlainsLayout.STYLES.get(portal.region()).minLevel());
            case "donjon" -> RpgPlayers.get(p).level >= com.mmorpg.dungeon.DungeonMaster.requiredLevel(DungeonType.byId(portal.dungeon()), portal.difficulty());
            default -> false;
        };
    }
    public static void enter(ServerPlayer p, Portal portal) {
        if (!allowed(p, portal)) { fail(p, "Accès refusé : choisissez votre classe, respectez le niveau et découvrez d'abord cette région. Terminez aussi tout essai, cinématique ou combat PvP."); return; }
        PortalAccess.grant(p, portal.kind());
        switch (portal.kind()) {
            case "hub" -> com.mmorpg.hub.HubManager.goToHub(p);
            case "spawn" -> com.mmorpg.hub.HubManager.goToSpawn(p);
            case "mine" -> { if (portal.region() < 0) com.mmorpg.mine.MineManager.open(p, null); else com.mmorpg.mine.MineManager.startTransition(p, portal.region()); }
            case "plaines" -> { if (portal.region() < 0) com.mmorpg.plains.PlainsManager.open(p); else com.mmorpg.plains.PlainsManager.enterChosen(p, portal.region()); }
            case "donjon" -> com.mmorpg.dungeon.InstanceManager.enterPool(p, com.mmorpg.dungeon.InstanceManager.pool(DungeonType.byId(portal.dungeon()), portal.difficulty()));
        }
    }
    /** Returning from an instance inside its entrance must not immediately start another run. */
    public static boolean visit(ServerPlayer p, Portal inside) {
        UUID previous = INSIDE.get(p.getUUID());
        if (inside == null) {
            Portal old = previous == null ? null : AdventurePortalData.get(p.level().getServer()).find(previous);
            // Clear only after physically stepping out near the source arch. Keep the marker while away in an instance.
            if (old == null || (old.dimension().equals(p.level().dimension()) && p.distanceToSqr(old.pos().getX() + .5, old.pos().getY(), old.pos().getZ() + .5) < 64)) INSIDE.remove(p.getUUID());
            return false;
        }
        INSIDE.put(p.getUUID(), inside.id());
        return !inside.id().equals(previous) && p.tickCount >= 40;
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 8 != 0) return;
        var portals = AdventurePortalData.get(server).all();
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            Portal inside = null;
            for (Portal portal : portals) {
                if (!valid(portal) || !p.level().dimension().equals(portal.dimension()) || p.distanceToSqr(portal.pos().getX() + .5, portal.pos().getY() + 2, portal.pos().getZ() + .5) > 32 * 32) continue;
                if (!frame(portal).entrySet().stream().allMatch(e -> p.level().getBlockState(e.getKey()).equals(e.getValue()))) continue;
                int color = portal.kind().equals("mine") ? 0xFFB040 : portal.kind().equals("plaines") ? 0x50E0A0 : 0xB070FF;
                p.level().sendParticles(p, new DustParticleOptions(color, 1.3f), false, false, portal.pos().getX() + .5, portal.pos().getY() + 2, portal.pos().getZ() + .5, 12, portal.widthX() ? 1.2 : .1, 1.5, portal.widthX() ? .1 : 1.2, 0);
                if (p.distanceToSqr(portal.pos().getX() + .5, portal.pos().getY(), portal.pos().getZ() + .5) < 36 && server.getTickCount() % 40 == 0) p.sendOverlayMessage(Component.literal(label(portal)).withColor(color));
                if (passage(portal).contains(p.position())) inside = portal;
            }
            if (visit(p, inside)) enter(p, inside);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { INSIDE.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { INSIDE.clear(); HISTORY.clear(); }

    private static int create(net.minecraft.commands.CommandSourceStack source, String kind, int region, String dungeon, int difficulty) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Direction facing = p.getDirection();
        Portal portal = new Portal(UUID.randomUUID(), p.level().dimension(), p.blockPosition().relative(facing, 4), facing.getAxis() == Direction.Axis.Z, kind, region, dungeon, difficulty);
        if (!place(p, portal)) return 0;
        source.sendSuccess(() -> Component.literal("Portail posé : " + label(portal) + ". Traversez l'arche pour entrer. Annuler : /undoportail."), true);
        return 1;
    }
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void commands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        var root = Commands.literal("portail").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.then(Commands.literal("hub").executes(c -> create(c.getSource(), "hub", -1, "", 0)));
        root.then(Commands.literal("spawn").executes(c -> create(c.getSource(), "spawn", -1, "", 0)));
        root.then(Commands.literal("mine").executes(c -> create(c.getSource(), "mine", -1, "", 0))
                .then(Commands.argument("region", IntegerArgumentType.integer(1, com.mmorpg.mine.MineLayout.SECTORS)).executes(c -> create(c.getSource(), "mine", IntegerArgumentType.getInteger(c, "region") - 1, "", 0))));
        root.then(Commands.literal("plaines").executes(c -> create(c.getSource(), "plaines", -1, "", 0))
                .then(Commands.argument("region", IntegerArgumentType.integer(1, com.mmorpg.plains.PlainsLayout.REGIONS)).executes(c -> create(c.getSource(), "plaines", IntegerArgumentType.getInteger(c, "region") - 1, "", 0))));
        var dungeon = Commands.argument("type", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(DungeonType.values()).map(t -> t.id), b))
                .executes(c -> create(c.getSource(), "donjon", -1, StringArgumentType.getString(c, "type"), 0));
        for (int i = 0; i < 3; i++) {
            final int difficulty = i;
            dungeon.then(Commands.literal(new String[]{"normal", "heroique", "mythique"}[i]).executes(c -> create(c.getSource(), "donjon", -1, StringArgumentType.getString(c, "type"), difficulty)));
        }
        root.then(Commands.literal("donjon").then(dungeon));
        root.then(Commands.literal("liste").executes(c -> {
            var portals = AdventurePortalData.get(c.getSource().getServer()).all();
            c.getSource().sendSuccess(() -> Component.literal(portals.size() + " portail(s) enregistré(s)."), false);
            for (Portal portal : portals) c.getSource().sendSuccess(() -> Component.literal(portal.id().toString().substring(0, 8) + " · " + (valid(portal) ? label(portal) : "invalide") + " · " + portal.dimension().identifier() + " " + portal.pos().toShortString()), false);
            return portals.size();
        }));
        root.then(Commands.literal("supprimer").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Portal nearest = AdventurePortalData.get(p.level().getServer()).all().stream().filter(g -> g.dimension().equals(p.level().dimension()) && p.distanceToSqr(g.pos().getX() + .5, g.pos().getY(), g.pos().getZ() + .5) < 64).min(Comparator.comparingDouble(g -> p.distanceToSqr(g.pos().getX() + .5, g.pos().getY(), g.pos().getZ() + .5))).orElse(null);
            if (nearest == null) { fail(p, "Aucun portail à moins de 8 blocs."); return 0; }
            if (!remove(p, nearest)) return 0;
            c.getSource().sendSuccess(() -> Component.literal("Portail supprimé. /undoportail permet de le rétablir."), true);
            return 1;
        }));
        dispatcher.register(root);
        var undo = dispatcher.register(Commands.literal("undoportail").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> { boolean ok = undo(c.getSource().getPlayerOrException()); if (ok) c.getSource().sendSuccess(() -> Component.literal("Dernier aménagement annulé ; état précédent restauré."), true); return ok ? 1 : 0; })
                .then(Commands.argument("nombre", IntegerArgumentType.integer(1, MAX_HISTORY)).executes(c -> {
                    int count = 0;
                    for (int i = 0; i < IntegerArgumentType.getInteger(c, "nombre") && undo(c.getSource().getPlayerOrException()); i++) count++;
                    final int total = count; c.getSource().sendSuccess(() -> Component.literal(total + " modification(s) de portail annulée(s)."), true); return count;
                })));
        if (dispatcher.getRoot().getChild("undo") == null) dispatcher.register(Commands.literal("undo").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(undo.getCommand()).redirect(undo));
    }
}
