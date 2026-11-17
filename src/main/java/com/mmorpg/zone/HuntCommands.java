package com.mmorpg.zone;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * /zone chasse (operateurs) : zones de chasse/recolte de l'openworld (voir {@link HuntZones}). Creation a partir de la selection de la
 * baguette de zone, reglages un par un, rechargement du fichier {@code config/mmorpg/zones_chasse.json}.
 */
public final class HuntCommands {
    private HuntCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var ids = (com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>) (c, b) -> SharedSuggestionProvider.suggest(HuntZones.all().stream().map(z -> z.id).toList(), b);
        d.register(Commands.literal("quetes").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("recharger").executes(c -> {
                    com.mmorpg.config.ConfigManager.load();
                    int n = com.mmorpg.config.ConfigManager.quests().size();
                    c.getSource().sendSuccess(() -> ok(n + " quêtes rechargées depuis config/mmorpg/quests.json"), true);
                    return n;
                })));
        d.register(Commands.literal("zone").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("chasse")
                        .then(Commands.literal("creer").then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("niveauMin", IntegerArgumentType.integer(1, 200))
                                        .then(Commands.argument("niveauMax", IntegerArgumentType.integer(1, 200))
                                                .then(Commands.argument("monstres", StringArgumentType.greedyString()).executes(HuntCommands::create))))))
                        .then(Commands.literal("liste").executes(HuntCommands::list))
                        .then(Commands.literal("recharger").executes(c -> {
                            int n = HuntZones.reload();
                            c.getSource().sendSuccess(() -> ok(n + " zone(s) de chasse rechargée(s) depuis " + HuntZones.file().getFileName()), true);
                            return n;
                        }))
                        .then(Commands.literal("supprimer").then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(c -> {
                            String id = StringArgumentType.getString(c, "id");
                            try {
                                boolean ok = HuntZones.remove(id);
                                if (!ok) c.getSource().sendFailure(Component.literal("Zone inconnue : " + id));
                                else c.getSource().sendSuccess(() -> ok("Zone de chasse « " + id + " » supprimée."), true);
                                return ok ? 1 : 0;
                            } catch (java.io.IOException e) {
                                c.getSource().sendFailure(Component.literal("Écriture impossible : " + e.getMessage()));
                                return 0;
                            }
                        })))
                        .then(Commands.literal("monstres").then(Commands.argument("id", StringArgumentType.word()).suggests(ids)
                                .then(Commands.argument("liste", StringArgumentType.greedyString())
                                        .executes(c -> edit(c, z -> z.monstres = mobs(StringArgumentType.getString(c, "liste")))))))
                        .then(Commands.literal("niveaux").then(Commands.argument("id", StringArgumentType.word()).suggests(ids)
                                .then(Commands.argument("min", IntegerArgumentType.integer(1, 200))
                                        .then(Commands.argument("max", IntegerArgumentType.integer(1, 200)).executes(c -> edit(c, z -> {
                                            z.niveauMin = IntegerArgumentType.getInteger(c, "min");
                                            z.niveauMax = Math.max(z.niveauMin, IntegerArgumentType.getInteger(c, "max"));
                                        }))))))
                        .then(Commands.literal("max").then(Commands.argument("id", StringArgumentType.word()).suggests(ids)
                                .then(Commands.argument("nombre", IntegerArgumentType.integer(1, 200))
                                        .executes(c -> edit(c, z -> z.max = IntegerArgumentType.getInteger(c, "nombre"))))))
                        .then(Commands.literal("sure").then(Commands.argument("id", StringArgumentType.word()).suggests(ids)
                                .then(Commands.argument("rayon", IntegerArgumentType.integer(0, 500))
                                        .executes(c -> edit(c, z -> z.surRayon = IntegerArgumentType.getInteger(c, "rayon"))))))
                        .then(Commands.literal("recolte").then(Commands.argument("id", StringArgumentType.word()).suggests(ids)
                                .then(Commands.argument("valeur", BoolArgumentType.bool())
                                        .executes(c -> edit(c, z -> z.recolte = BoolArgumentType.getBool(c, "valeur"))))))
                        .then(Commands.literal("actif").then(Commands.argument("id", StringArgumentType.word()).suggests(ids)
                                .then(Commands.argument("valeur", BoolArgumentType.bool())
                                        .executes(c -> edit(c, z -> z.actif = BoolArgumentType.getBool(c, "valeur"))))))
                        .then(Commands.literal("nom").then(Commands.argument("id", StringArgumentType.word()).suggests(ids)
                                .then(Commands.argument("nom", StringArgumentType.greedyString())
                                        .executes(c -> edit(c, z -> z.nom = StringArgumentType.getString(c, "nom").trim())))))
                        .then(Commands.literal("tp").then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(HuntCommands::teleport)))));
    }

    private static Component ok(String s) {
        return Component.literal(s).withColor(0xA0E0A0);
    }

    private static List<String> mobs(String csv) {
        List<String> out = new ArrayList<>();
        Arrays.stream(csv.split("[,\\s]+")).map(String::trim).filter(s -> !s.isEmpty()).forEach(out::add);
        return out;
    }

    private static int create(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String id = ZoneData.slug(StringArgumentType.getString(c, "id"));
        if (id.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Identifiant invalide."));
            return 0;
        }
        ZoneManager.Selection s = ZoneManager.selection(p);
        if (!s.complete() || !p.level().dimension().equals(s.dimension)) {
            c.getSource().sendFailure(Component.literal("Sélectionnez deux coins avec la baguette de zone (clic droit sur un bloc, deux fois)."));
            return 0;
        }
        var zone = new HuntZones.Zone();
        zone.id = id;
        zone.nom = id;
        zone.dimension = s.dimension.identifier().toString();
        zone.x1 = Math.min(s.a.getX(), s.b.getX());
        zone.x2 = Math.max(s.a.getX(), s.b.getX());
        zone.z1 = Math.min(s.a.getZ(), s.b.getZ());
        zone.z2 = Math.max(s.a.getZ(), s.b.getZ());
        zone.y1 = Math.min(s.a.getY(), s.b.getY()) - 8;                               // marge verticale : on chasse un peu sous/au-dessus des coins
        zone.y2 = Math.max(s.a.getY(), s.b.getY()) + 24;
        zone.niveauMin = IntegerArgumentType.getInteger(c, "niveauMin");
        zone.niveauMax = Math.max(zone.niveauMin, IntegerArgumentType.getInteger(c, "niveauMax"));
        zone.monstres = mobs(StringArgumentType.getString(c, "monstres"));
        try {
            HuntZones.put(zone);
        } catch (java.io.IOException e) {
            c.getSource().sendFailure(Component.literal("Écriture impossible : " + e.getMessage()));
            return 0;
        }
        ZoneManager.clearSelection(p);
        c.getSource().sendSuccess(() -> ok("Zone de chasse « " + id + " » créée : niveaux " + zone.niveauMin + "-" + zone.niveauMax + ", " + zone.monstres
                + ". Réglages : /zone chasse <monstres|niveaux|max|sure|recolte|actif|nom> " + id + " …"), true);
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> c, Consumer<HuntZones.Zone> change) {
        String id = StringArgumentType.getString(c, "id");
        var found = HuntZones.get(id);
        if (found.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Zone inconnue : " + id));
            return 0;
        }
        try {
            change.accept(found.get());
            HuntZones.put(found.get());
        } catch (java.io.IOException e) {
            c.getSource().sendFailure(Component.literal("Écriture impossible : " + e.getMessage()));
            return 0;
        }
        c.getSource().sendSuccess(() -> ok("Zone « " + id + " » mise à jour : " + describe(found.get())), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        if (HuntZones.all().isEmpty()) {
            c.getSource().sendSuccess(() -> Component.literal("Aucune zone de chasse. /zone chasse creer <id> <niveauMin> <niveauMax> <monstres> (après sélection à la baguette de zone) ou éditez " + HuntZones.file().getFileName()), false);
            return 0;
        }
        for (var z : HuntZones.all()) c.getSource().sendSuccess(() -> Component.literal("• " + z.id + " — " + describe(z)), false);
        return HuntZones.all().size();
    }

    private static String describe(HuntZones.Zone z) {
        return z.title() + " [" + z.dimension + " " + z.x1 + "," + z.z1 + " → " + z.x2 + "," + z.z2 + "] niv. " + z.niveauMin + "-" + z.niveauMax + ", " + z.monstres
                + ", max " + z.max + (z.surRayon > 0 ? ", sûre " + z.surRayon : "") + (z.recolte ? ", récolte" : "") + (z.actif ? "" : " (inactive)");
    }

    private static int teleport(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        var z = HuntZones.get(StringArgumentType.getString(c, "id"));
        if (z.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Zone inconnue."));
            return 0;
        }
        var level = p.level().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.Identifier.parse(z.get().dimension)));
        if (level == null) {
            c.getSource().sendFailure(Component.literal("Dimension introuvable : " + z.get().dimension));
            return 0;
        }
        var ctr = z.get().center();
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) ctr.x, (int) ctr.z);
        p.teleportTo(level, ctr.x, y, ctr.z, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
        return 1;
    }
}
