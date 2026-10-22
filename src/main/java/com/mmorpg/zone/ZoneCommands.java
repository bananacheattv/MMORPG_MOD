package com.mmorpg.zone;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** /zone (operateurs) : creer a partir de la selection de la baguette, lister, regler, supprimer, savoir ou l'on se trouve. */
public final class ZoneCommands {
    private static final List<String> RULES = List.of("construction", "pvp", "monstres");
    private static final List<String> VALUES = List.of("interdit", "autorise");

    private ZoneCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("zone")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("creer").then(Commands.argument("nom", StringArgumentType.greedyString()).executes(ZoneCommands::create)))
                .then(Commands.literal("liste").executes(ZoneCommands::list))
                .then(Commands.literal("ici").executes(ZoneCommands::here))
                .then(Commands.literal("supprimer").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(c), b))
                        .executes(ZoneCommands::remove)))
                .then(Commands.literal("style").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(c), b))
                        .then(Commands.argument("style", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("zone", "boss"), b))
                                .executes(c -> style(c, null))
                                .then(Commands.argument("soustitre", StringArgumentType.greedyString()).executes(c -> style(c, StringArgumentType.getString(c, "soustitre")))))))
                .then(Commands.literal("regle").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(c), b))
                        .then(Commands.argument("regle", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(RULES, b))
                                .then(Commands.argument("valeur", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(VALUES, b))
                                        .executes(ZoneCommands::rule))))));
    }

    private static List<String> ids(CommandContext<CommandSourceStack> c) {
        return ZoneData.get(c.getSource().getServer()).all().stream().map(ZoneData.Zone::id).toList();
    }

    private static Component ok(String s) {
        return Component.literal(s).withColor(0xA0E0A0);
    }

    private static int create(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "nom").trim();
        String id = ZoneData.slug(name);
        if (id.isEmpty() || name.length() > 60) {
            c.getSource().sendFailure(Component.literal("Nom invalide (1 à 60 caractères, au moins une lettre ou un chiffre)."));
            return 0;
        }
        ZoneManager.Selection s = ZoneManager.selection(p);
        if (!s.complete() || !p.level().dimension().equals(s.dimension)) {
            c.getSource().sendFailure(Component.literal("Sélectionnez deux coins avec la baguette de zone (clic droit sur un bloc, deux fois)."));
            return 0;
        }
        ZoneData data = ZoneData.get(c.getSource().getServer());
        if (data.byId(id) != null) {
            c.getSource().sendFailure(Component.literal("Une zone « " + id + " » existe déjà : /zone supprimer " + id + " d'abord."));
            return 0;
        }
        var a = s.a;
        var b = s.b;
        ZoneData.Zone zone = new ZoneData.Zone(id, name, s.dimension, Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getZ(), b.getZ()), true, true, true, ZoneData.Zone.STYLE_ZONE, "");
        data.add(zone);
        ZoneManager.clearSelection(p);
        c.getSource().sendSuccess(() -> ok("Zone « " + name + " » créée (" + zone.area() + " blocs de surface) : " + ZoneManager.describe(zone)
                + ". Réglages : /zone regle " + id + " <construction|pvp|monstres> <interdit|autorise>"), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        List<ZoneData.Zone> zones = ZoneData.get(c.getSource().getServer()).all();
        if (zones.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.literal("Aucune zone. Prenez la baguette de zone (onglet créatif « Objets »)."), false);
            return 0;
        }
        for (ZoneData.Zone z : zones) {
            c.getSource().sendSuccess(() -> Component.literal("• " + z.id() + " — " + z.name() + " [" + z.minX() + "," + z.minZ() + " → " + z.maxX() + "," + z.maxZ()
                    + "] " + ZoneManager.describe(z)), false);
        }
        return zones.size();
    }

    private static int here(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ZoneData.Zone z = ZoneManager.zoneAt(p.level(), p.blockPosition());
        c.getSource().sendSuccess(() -> z == null ? Component.literal("Vous n'êtes dans aucune zone.")
                : ok(z.name() + " (" + z.id() + ") : " + ZoneManager.describe(z)), false);
        return z == null ? 0 : 1;
    }

    private static int remove(CommandContext<CommandSourceStack> c) {
        String id = StringArgumentType.getString(c, "id");
        if (!ZoneData.get(c.getSource().getServer()).remove(id)) {
            c.getSource().sendFailure(Component.literal("Zone inconnue : " + id));
            return 0;
        }
        c.getSource().sendSuccess(() -> ok("Zone « " + id + " » supprimée."), true);
        return 1;
    }

    /** /zone style <id> <zone|boss> [sous-titre] : animation d'entree (filet dore ou salle de boss) et ligne affichee sous le titre. */
    private static int style(CommandContext<CommandSourceStack> c, String subtitle) {
        ZoneData data = ZoneData.get(c.getSource().getServer());
        String id = StringArgumentType.getString(c, "id"), style = StringArgumentType.getString(c, "style");
        ZoneData.Zone z = data.byId(id);
        if (z == null || !(style.equals("zone") || style.equals("boss"))) {
            c.getSource().sendFailure(Component.literal("Usage : /zone style <id> <zone|boss> [sous-titre]"));
            return 0;
        }
        data.add(z.withStyle(style, subtitle == null ? z.subtitle() : subtitle));
        c.getSource().sendSuccess(() -> ok(z.name() + " : animation « " + style + " »" + (subtitle == null ? "" : ", sous-titre « " + subtitle + " »")), true);
        return 1;
    }

    private static int rule(CommandContext<CommandSourceStack> c) {
        ZoneData data = ZoneData.get(c.getSource().getServer());
        String id = StringArgumentType.getString(c, "id"), rule = StringArgumentType.getString(c, "regle"), value = StringArgumentType.getString(c, "valeur");
        ZoneData.Zone z = data.byId(id);
        if (z == null || !RULES.contains(rule) || !VALUES.contains(value)) {
            c.getSource().sendFailure(Component.literal("Usage : /zone regle <id> <construction|pvp|monstres> <interdit|autorise>"));
            return 0;
        }
        boolean forbid = value.equals("interdit");
        ZoneData.Zone updated = switch (rule) {
            case "construction" -> z.with(forbid, z.noPvp(), z.noMobs());
            case "pvp" -> z.with(z.noBuild(), forbid, z.noMobs());
            default -> z.with(z.noBuild(), z.noPvp(), forbid);
        };
        data.add(updated);
        c.getSource().sendSuccess(() -> ok(z.name() + " : " + ZoneManager.describe(updated)), true);
        return 1;
    }
}
