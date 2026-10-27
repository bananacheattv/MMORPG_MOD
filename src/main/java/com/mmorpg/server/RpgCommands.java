package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.entity.boss.BossManager;
import com.mmorpg.pet.PetType;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.LevelSystem;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import com.mmorpg.world.DungeonBuilder;
import com.mmorpg.world.WaypointData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Commandes d'administration : /mmorpg ... */
public final class RpgCommands {
    private RpgCommands() {
    }

    private static Component ok(String msg) {
        return Component.literal("[MMORPG] ").withColor(0xE8C060).append(Component.literal(msg).withColor(0xE0D8C0));
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        List<String> classes = Arrays.stream(PlayerClass.values()).map(c -> c == PlayerClass.TANK ? "paladin" : c.id()).toList();
        List<String> bosses = List.of("roi_gobelin", "liche_ancienne", "seigneur_ignis", "titan_glace", "avatar_neant");
        List<String> mobs = new ArrayList<>(ModEntities.SPAWNABLE.keySet());
        List<String> dungeons = Arrays.stream(DungeonBuilder.DungeonType.values()).map(t -> t.id).toList();
        List<String> pets = new ArrayList<>(Arrays.stream(PetType.values()).map(p -> p.id).toList());
        pets.add("tous");
        List<String> cosmetics = new ArrayList<>(Arrays.stream(Cosmetic.values()).map(c -> c.id).toList());
        cosmetics.add("tous");

        d.register(Commands.literal("mmorpg")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("editeurquetes").executes(c -> {QuestEditor.open(c.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("butins").executes(c -> {com.mmorpg.server.LootEditor.open(c.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("niveau").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.argument("niveau", IntegerArgumentType.integer(1, LevelSystem.MAX_LEVEL)).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                            int lvl = IntegerArgumentType.getInteger(c, "niveau");
                            RpgPlayers.setLevel(p, lvl);
                            c.getSource().sendSuccess(() -> ok(p.getName().getString() + " est maintenant niveau " + lvl), true);
                            return 1;
                        }))))
                .then(Commands.literal("xp").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.argument("montant", IntegerArgumentType.integer(1)).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                            int xp = IntegerArgumentType.getInteger(c, "montant");
                            RpgPlayers.giveXp(p, xp, false);
                            c.getSource().sendSuccess(() -> ok(xp + " XP donnés à " + p.getName().getString()), true);
                            return 1;
                        }))))
                .then(Commands.literal("classe").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.argument("classe", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(classes, b))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                                    PlayerClass cls = PlayerClass.byName(StringArgumentType.getString(c, "classe"));
                                    PlayerData data = RpgPlayers.get(p);
                                    data.resetClass();
                                    if (cls.isPlayable()) {
                                        data.classChosenOnce = true;
                                        RpgPlayers.selectClass(p, cls);
                                    } else {
                                        RpgPlayers.recomputeAndSync(p);
                                        RpgPlayers.openClassSelection(p);
                                    }
                                    c.getSource().sendSuccess(() -> ok("Classe de " + p.getName().getString() + " : " + cls.label), true);
                                    return 1;
                                }))))
                .then(Commands.literal("reset").then(Commands.argument("joueur", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                    PlayerData data = RpgPlayers.get(p);
                    PetManager.despawn(p);
                    data.load(new PlayerData().save());
                    data.hp = -1;
                    data.mana = -1;
                    RpgPlayers.recomputeAndSync(p);
                    RpgPlayers.openClassSelection(p);
                    c.getSource().sendSuccess(() -> ok("Progression de " + p.getName().getString() + " réinitialisée"), true);
                    return 1;
                })))
                .then(Commands.literal("points").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.argument("nombre", IntegerArgumentType.integer(1, 1000)).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                            int n = IntegerArgumentType.getInteger(c, "nombre");
                            RpgPlayers.get(p).attributePoints += n;
                            RpgPlayers.sync(p);
                            c.getSource().sendSuccess(() -> ok(n + " points d'attribut donnés"), true);
                            return 1;
                        }))))
                .then(Commands.literal("soin").then(Commands.argument("joueur", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                    RpgPlayers.fullRestore(p);
                    p.getData(com.mmorpg.registry.ModAttachments.PLAYER).cooldowns.clear();
                    RpgPlayers.sync(p);
                    c.getSource().sendSuccess(() -> ok("PV, mana et recharges restaurés"), true);
                    return 1;
                })))
                .then(Commands.literal("info").then(Commands.argument("joueur", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                    PlayerData data = RpgPlayers.get(p);
                    StringBuilder sb = new StringBuilder();
                    sb.append(p.getName().getString()).append(" — ").append(data.title()).append(" niv. ").append(data.level)
                            .append(" (").append(data.xp).append("/").append(LevelSystem.xpToNext(data.level)).append(" XP)\n");
                    for (Stat s : Stat.values()) {
                        sb.append(s.shortName).append(' ').append(s.format(data.stats.get(s), false)).append("  ");
                    }
                    c.getSource().sendSuccess(() -> ok(sb.toString()), false);
                    return 1;
                })))
                .then(Commands.literal("boss").then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(bosses, b))
                        .executes(c -> {
                            String key = StringArgumentType.getString(c, "type");
                            EntityType<? extends Mob> type = BossManager.typeFor(key);
                            if (type == null) {
                                c.getSource().sendFailure(Component.literal("Boss inconnu : " + key));
                                return 0;
                            }
                            ServerLevel level = c.getSource().getLevel();
                            BossManager.spawnBoss(level, type, key, BlockPos.containing(c.getSource().getPosition()), c.getSource().getTextName());
                            return 1;
                        })))
                .then(Commands.literal("mob").then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(mobs, b))
                        .executes(c -> spawnMob(c, 0))
                        .then(Commands.argument("niveau", IntegerArgumentType.integer(1, 100))
                                .executes(c -> spawnMob(c, IntegerArgumentType.getInteger(c, "niveau"))))))
                .then(Commands.literal("donjon")
                        .then(Commands.literal("reinitialiser").executes(c -> {
                            ServerLevel lv = c.getSource().getLevel();
                            String g = com.mmorpg.dungeon.DungeonManager.groupAt(lv, BlockPos.containing(c.getSource().getPosition()));
                            if (g == null) {
                                c.getSource().sendFailure(Component.literal("Placez-vous dans un donjon pour le réinitialiser."));
                                return 0;
                            }
                            int n = com.mmorpg.dungeon.DungeonManager.reset(c.getSource().getServer(), g);
                            c.getSource().sendSuccess(() -> ok("Donjon réinitialisé : " + n + " épreuve(s) remise(s) à zéro (herses refermées)."), true);
                            return 1;
                        }))
                        .then(Commands.literal("construire").then(typeArg(dungeons, RpgCommands::buildDungeonCommand)))
                        .then(Commands.literal("prechauffer").then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(dungeons, b))
                                .then(Commands.argument("nombre", IntegerArgumentType.integer(1, 20)).executes(c -> {
                                    DungeonBuilder.DungeonType t = DungeonBuilder.DungeonType.byId(StringArgumentType.getString(c, "type"));
                                    if (t == null) {
                                        c.getSource().sendFailure(Component.literal("Donjon inconnu"));
                                        return 0;
                                    }
                                    int n = com.mmorpg.dungeon.InstanceManager.prewarm(c.getSource().getServer(), t, 0, IntegerArgumentType.getInteger(c, "nombre"));
                                    c.getSource().sendSuccess(() -> ok(n + " exemplaire(s) de « " + t.label + " » construit(s) d'avance."), true);
                                    return n;
                                }))))
                        .then(typeArg(dungeons, RpgCommands::createGateCommand)))
                .then(Commands.literal("ville").then(Commands.argument("nom", StringArgumentType.greedyString()).executes(c -> {
                    String name = StringArgumentType.getString(c, "nom");
                    BlockPos tp = DungeonBuilder.buildCity(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), name);
                    c.getSource().sendSuccess(() -> ok("Ville « " + name + " » créée. Téléporteur en " + tp.toShortString()), true);
                    return 1;
                })))
                .then(Commands.literal("teleporteurs").executes(c -> {
                    WaypointData data = WaypointData.get(c.getSource().getServer());
                    StringBuilder sb = new StringBuilder("Téléporteurs (" + data.all().size() + ") :");
                    for (WaypointData.Waypoint w : data.all()) {
                        sb.append("\n - ").append(w.name).append(" [").append(WaypointData.CATEGORY_NAMES[Math.max(0, Math.min(2, w.category))])
                                .append(", niv. ").append(w.minLevel).append("] ").append(WaypointData.dimensionName(w.dimension)).append(' ').append(w.pos.toShortString());
                    }
                    c.getSource().sendSuccess(() -> ok(sb.toString()), false);
                    return 1;
                }))
                .then(Commands.literal("familier").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(pets, b))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                                    String id = StringArgumentType.getString(c, "id");
                                    for (PetType t : PetType.values()) {
                                        if (id.equals("tous") || t.id.equals(id)) RpgPlayers.unlockPet(p, t);
                                    }
                                    c.getSource().sendSuccess(() -> ok("Familier(s) débloqué(s)"), true);
                                    return 1;
                                }))))
                .then(Commands.literal("cosmetique").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(cosmetics, b))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                                    String id = StringArgumentType.getString(c, "id");
                                    PlayerData data = RpgPlayers.get(p);
                                    for (Cosmetic co : Cosmetic.values()) {
                                        if (id.equals("tous") || co.id.equals(id)) data.cosmetics.add(co.id);
                                    }
                                    RpgPlayers.sync(p);
                                    c.getSource().sendSuccess(() -> ok("Cosmétique(s) débloqué(s)"), true);
                                    return 1;
                                }))))
                .then(Commands.literal("hub").executes(RpgCommands::buildHub))
                .then(Commands.literal("cinematique")
                        .executes(c -> playCinematic(c.getSource().getPlayerOrException(), c))
                        .then(Commands.argument("joueur", EntityArgument.player()).executes(c -> playCinematic(EntityArgument.getPlayer(c, "joueur"), c))))
                .then(Commands.literal("essai").then(Commands.literal("fin")
                        .executes(c -> endTrial(c.getSource().getPlayerOrException(), c))
                        .then(Commands.argument("joueur", EntityArgument.player()).executes(c -> endTrial(EntityArgument.getPlayer(c, "joueur"), c)))))
                .then(Commands.literal("mannequin")
                        .then(Commands.literal("supprimer").executes(c -> {
                            var list = c.getSource().getLevel().getEntitiesOfClass(com.mmorpg.entity.dummy.TrainingDummyEntity.class,
                                    new net.minecraft.world.phys.AABB(BlockPos.containing(c.getSource().getPosition())).inflate(10));
                            list.forEach(n -> n.discard());
                            c.getSource().sendSuccess(() -> ok(list.size() + " mannequin(s) supprimé(s)"), true);
                            return list.size();
                        }))
                        .then(Commands.argument("variante", StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(com.mmorpg.entity.dummy.TrainingDummyEntity.VARIANTS, b))
                                .executes(c -> {
                                    String id = StringArgumentType.getString(c, "variante");
                                    int v = java.util.Arrays.asList(com.mmorpg.entity.dummy.TrainingDummyEntity.VARIANTS).indexOf(id);
                                    if (v < 0) {
                                        c.getSource().sendFailure(Component.literal("Variante inconnue : standard, blinde ou magique."));
                                        return 0;
                                    }
                                    var level = c.getSource().getLevel();
                                    var dummy = com.mmorpg.registry.ModEntities.TRAINING_DUMMY.get().create(level, null, BlockPos.containing(c.getSource().getPosition()),
                                            net.minecraft.world.entity.EntitySpawnReason.COMMAND, true, false);
                                    if (dummy == null) return 0;
                                    var pos = c.getSource().getPosition();
                                    dummy.snapTo(pos.x, pos.y, pos.z, c.getSource().getRotation().y + 180, 0);
                                    dummy.setup(v);
                                    level.addFreshEntity(dummy);
                                    c.getSource().sendSuccess(() -> ok("Mannequin « " + dummy.config().name + " » posé."), true);
                                    return 1;
                                })))
                .then(Commands.literal("pnj")
                        .then(Commands.literal("quetes_groupe")
                            .then(Commands.argument("groupe", StringArgumentType.word())
                            .then(Commands.argument("apparence", IntegerArgumentType.integer(0, 3))
                            .then(Commands.argument("nom", StringArgumentType.greedyString()).executes(c -> {
                                String name = StringArgumentType.getString(c, "nom");
                                int skin = IntegerArgumentType.getInteger(c, "apparence");
                                String group = StringArgumentType.getString(c, "groupe");
                                var npc = DungeonBuilder.spawnNpc(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), com.mmorpg.entity.NpcEntity.Role.QUETES, name, skin, c.getSource().getRotation().y + 180);
                                if (npc == null) return 0;
                                npc.setup(com.mmorpg.entity.NpcEntity.Role.QUETES, name, skin, group);
                                c.getSource().sendSuccess(() -> ok("PNJ « " + name + " » créé, groupe : " + group), true);
                                return 1;
                            })))))
                        .then(Commands.literal("maitre_classes").executes(c -> {
                            // PNJ du groupe « maitre_classes » : propose les quetes d'evolution (modifiables dans /mmorpg quetes editeur)
                            String name = "Maître des classes";
                            var npc = DungeonBuilder.spawnNpc(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), com.mmorpg.entity.NpcEntity.Role.QUETES, name, 2, c.getSource().getRotation().y + 180);
                            if (npc == null) return 0;
                            npc.setup(com.mmorpg.entity.NpcEntity.Role.QUETES, name, 2, "maitre_classes");
                            c.getSource().sendSuccess(() -> ok("Maître des classes créé (quêtes d'évolution)."), true);
                            return 1;
                        }))
                        .then(Commands.literal("quetes").then(Commands.argument("nom", StringArgumentType.greedyString()).executes(c -> spawnNpc(c, com.mmorpg.entity.NpcEntity.Role.QUETES))))
                        .then(Commands.literal("marchand").then(Commands.argument("nom", StringArgumentType.greedyString()).executes(c -> spawnNpc(c, com.mmorpg.entity.NpcEntity.Role.MARCHAND))))
                        .then(Commands.literal("supprimer").executes(c -> {
                            var npcs = c.getSource().getLevel().getEntitiesOfClass(com.mmorpg.entity.NpcEntity.class,
                                    new net.minecraft.world.phys.AABB(BlockPos.containing(c.getSource().getPosition())).inflate(4));
                            npcs.forEach(n -> n.discard());
                            c.getSource().sendSuccess(() -> ok(npcs.size() + " PNJ supprimé(s)"), true);
                            return npcs.size();
                        })))
                .then(Commands.literal("or").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.argument("montant", IntegerArgumentType.integer(1)).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                            int amount = IntegerArgumentType.getInteger(c, "montant");
                            RpgPlayers.addGold(p, amount, true);
                            c.getSource().sendSuccess(() -> ok(amount + " or donnés à " + p.getName().getString()), true);
                            return 1;
                        }))))
                .then(Commands.literal("quetes").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.literal("reset").executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                            PlayerData data = RpgPlayers.get(p);
                            data.activeQuests.clear();
                            data.questGivers.clear();data.questGuidanceMuted=false;
                            GuidanceManager.refresh(p);
                            data.trackedQuest = "";
                            data.completedQuests.clear();
                            data.dailyQuests.clear();
                            RpgPlayers.sync(p);
                            c.getSource().sendSuccess(() -> ok("Quêtes de " + p.getName().getString() + " réinitialisées"), true);
                            return 1;
                        }))))
                .then(Commands.literal("reload").executes(c -> {
                    ConfigManager.load();
                    BestiaryInfo.sendToAll();
                    c.getSource().sendSuccess(() -> ok("Configuration rechargée (" + ConfigManager.mobs().size() + " monstres)"), true);
                    return 1;
                })));
    }

    private static int buildHub(CommandContext<CommandSourceStack> c) {
        var level = c.getSource().getServer().getLevel(com.mmorpg.hub.HubManager.HUB_DIM);
        if (level == null) {
            c.getSource().sendFailure(Component.literal("La dimension du hub (mmorpg:hub) est introuvable : le mod est-il à jour sur ce monde ?"));
            return 0;
        }
        c.getSource().sendSuccess(() -> ok("Construction de la Citadelle d'Aeloria en cours..."), false);
        com.mmorpg.hub.HubBuilder.build(level, com.mmorpg.hub.HubManager.HUB_ORIGIN);
        c.getSource().sendSuccess(() -> ok("Hub d'Aeloria construit dans la dimension mmorpg:hub : rotonde, bassin, 4 pavillons de classe, portail, arène d'épreuve, aire d'entraînement, téléporteur. Point d'apparition du monde défini sur le hub : utilisez /hub pour vous y rendre."), true);
        return 1;
    }

    private static int setClass(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
        PlayerClass cls = PlayerClass.byName(StringArgumentType.getString(c, "classe"));
        PlayerData data = RpgPlayers.get(p);
        data.resetClass();
        if (cls.isPlayable()) {
            data.classChosenOnce = true;
            RpgPlayers.selectClass(p, cls);
        } else {
            RpgPlayers.recomputeAndSync(p);
            RpgPlayers.openClassSelection(p);
        }
        c.getSource().sendSuccess(() -> ok("Classe de " + p.getName().getString() + " : " + cls.label), true);
        return 1;
    }

    /** /hub (tous les joueurs), /hub set, /hub reset <joueur> et /class set <joueur> <classe> (operateurs). */
    public static void registerHub(CommandDispatcher<CommandSourceStack> d) {
        List<String> classes = Arrays.stream(PlayerClass.values()).map(c -> c == PlayerClass.TANK ? "paladin" : c.id()).toList();
        d.register(Commands.literal("hub")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!com.mmorpg.hub.HubManager.goToHub(p)) {
                        c.getSource().sendFailure(Component.literal("Le hub n'est pas encore construit (un opérateur doit utiliser /hub set)."));
                        return 0;
                    }
                    return 1;
                })
                .then(Commands.argument("numero", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 99)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "numero");
                    if (!com.mmorpg.hub.HubManager.goToHub(p, n - 1)) {
                        c.getSource().sendFailure(Component.literal("Le Hub " + n + " n'existe pas."));
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(RpgCommands::buildHub))
                .then(Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("joueur", EntityArgument.player()).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "joueur");
                            com.mmorpg.hub.HubManager.reset(p);
                            c.getSource().sendSuccess(() -> ok("Hub de " + p.getName().getString() + " réinitialisé (cinématique, essais et classe)."), true);
                            return 1;
                        }))));
        for (String name : new String[]{"spawn", "marche"}) {
            d.register(Commands.literal(name).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                if (!com.mmorpg.hub.HubManager.goToSpawn(p)) {
                    c.getSource().sendFailure(Component.literal("Le Spawn d'Aeloria n'est pas encore construit (un opérateur doit utiliser /hub set)."));
                    return 0;
                }
                return 1;
            })
                    .then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> SpawnEditor.setArrival(c.getSource().getPlayerOrException())))
                    .then(Commands.literal("pnj").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                        var spawn = com.mmorpg.hub.SpawnData.get(c.getSource().getServer());
                        var level = c.getSource().getServer().getLevel(com.mmorpg.hub.SpawnBuilder.DIM);
                        if (!spawn.exists() || level == null) return 0;
                        int n = com.mmorpg.hub.HubMarket.refreshNpcs(level, spawn.origin());
                        c.getSource().sendSuccess(() -> ok(n + " PNJ du spawn replacés d'après config/mmorpg/spawn.json."), true);
                        return n;
                    }))
                    .then(Commands.literal("stations").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                        var spawn = com.mmorpg.hub.SpawnData.get(c.getSource().getServer());
                        var level = c.getSource().getServer().getLevel(com.mmorpg.hub.SpawnBuilder.DIM);
                        if (!spawn.exists() || level == null) return 0;
                        int n = com.mmorpg.hub.HubMarket.refreshStations(level, spawn.origin());
                        c.getSource().sendSuccess(() -> ok(n + " station(s) du spawn posées d'après config/mmorpg/spawn.json (retirez à la main l'ancien emplacement d'une station déplacée)."), true);
                        return n;
                    }))
                    .then(Commands.literal("reconstruire").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                        var spawn = com.mmorpg.hub.SpawnData.get(c.getSource().getServer());
                        var level = c.getSource().getServer().getLevel(com.mmorpg.hub.SpawnBuilder.DIM);
                        if (level == null) return 0;
                        c.getSource().sendSuccess(() -> ok("Reconstruction de la Cité d'Aeloria (quelques secondes)…"), false);
                        com.mmorpg.hub.SpawnBuilder.build(level, spawn.exists() ? spawn.origin() : com.mmorpg.hub.HubManager.HUB_ORIGIN);
                        return 1;
                    })));
        }
        d.register(Commands.literal("class")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("set").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.argument("classe", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(classes, b))
                                .executes(RpgCommands::setClass)))));
    }

    private interface DungeonAction {
        int run(CommandContext<CommandSourceStack> c, int difficulty) throws com.mojang.brigadier.exceptions.CommandSyntaxException;
    }

    /** <type> [normal|heroique|mythique] : meme forme pour la construction directe et la creation d'une porte instanciee. */
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> typeArg(List<String> dungeons, DungeonAction action) {
        return Commands.argument("type", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(dungeons, b))
                .executes(c -> action.run(c, 0))
                .then(Commands.argument("difficulte", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("normal", "heroique", "mythique"), b))
                        .executes(c -> action.run(c, switch (StringArgumentType.getString(c, "difficulte")) {
                            case "heroique" -> 1;
                            case "mythique" -> 2;
                            default -> 0;
                        })));
    }

    /** /mmorpg donjon <type> [difficulte] : cree la porte du donjon ici ; chaque joueur ou groupe y entre dans son propre exemplaire. */
    private static int createGateCommand(CommandContext<CommandSourceStack> c, int difficulty) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        DungeonBuilder.DungeonType t = DungeonBuilder.DungeonType.byId(StringArgumentType.getString(c, "type"));
        if (t == null) {
            c.getSource().sendFailure(Component.literal("Donjon inconnu"));
            return 0;
        }
        ServerPlayer admin = c.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
        var w = com.mmorpg.dungeon.InstanceManager.createGate(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), t, difficulty, admin);
        c.getSource().sendSuccess(() -> ok("Porte du donjon « " + w.name + " » créée (niveau " + w.minLevel + " requis). Elle apparaît dans tous les téléporteurs ; chaque joueur ou groupe "
                + "entre dans son propre exemplaire, construit à la première utilisation (/mmorpg donjon prechauffer " + t.id + " <n> pour en préparer)."), true);
        return 1;
    }

    private static int buildDungeonCommand(CommandContext<CommandSourceStack> c, int difficulty) {
        DungeonBuilder.DungeonType t = DungeonBuilder.DungeonType.byId(StringArgumentType.getString(c, "type"));
        if (t == null) {
            c.getSource().sendFailure(Component.literal("Donjon inconnu"));
            return 0;
        }
        BlockPos tp = DungeonBuilder.buildDungeon(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), t, difficulty);
        String label = difficulty == 1 ? " héroïque" : difficulty >= 2 ? " mythique" : "";
        c.getSource().sendSuccess(() -> ok("Donjon « " + t.label + " »" + label + " (niv. " + t.level + ") construit vers le sud. Téléporteur en " + tp.toShortString()), true);
        return 1;
    }

    /** /setdonjon [type] : pose ici le Maitre des donjons (tous les donjons, ou un seul). Aucune autre preparation n'est necessaire. */
    public static void registerSetDonjon(CommandDispatcher<CommandSourceStack> d) {
        List<String> ids = Arrays.stream(DungeonBuilder.DungeonType.values()).map(t -> t.id).toList();
        d.register(Commands.literal("setdonjon")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> setDonjon(c, null))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids, b))
                        .executes(c -> {
                            var t = DungeonBuilder.DungeonType.byId(StringArgumentType.getString(c, "type"));
                            if (t == null) {
                                c.getSource().sendFailure(Component.literal("Donjon inconnu : " + StringArgumentType.getString(c, "type")));
                                return 0;
                            }
                            return setDonjon(c, t);
                        })));
    }

    /** /outils : redonne la pioche et la houe du debutant si on n'en possede plus. */
    public static void registerOutils(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("outils").executes(c -> {
            var sp = c.getSource().getPlayerOrException();
            boolean pick = false, hoe = false;
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                var st = sp.getInventory().getItem(i);
                if (st.is(com.mmorpg.registry.ModItems.get("pioche_debutant"))) pick = true;
                if (st.is(com.mmorpg.registry.ModItems.get("houe_debutant"))) hoe = true;
            }
            if (!pick) RpgPlayers.give(sp, com.mmorpg.registry.ModItems.get("pioche_debutant"), 1);
            if (!hoe) RpgPlayers.give(sp, com.mmorpg.registry.ModItems.get("houe_debutant"), 1);
            String msg = pick && hoe ? "Vous avez déjà vos outils du débutant." : "Outils du débutant remis.";
            c.getSource().sendSuccess(() -> ok(msg), false);
            return 1;
        }));
    }

    /** /setmine : pose ici le Maitre des mines (menu des etages, ecran de chargement et arrivee dans la mine). */
    public static void registerSetMine(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("setmine")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> {
                    var npc = DungeonBuilder.spawnNpc(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), com.mmorpg.entity.NpcEntity.Role.MINES,
                            "Maître des mines", 1, c.getSource().getRotation().y + 180);
                    if (npc == null) return 0;
                    npc.setup(com.mmorpg.entity.NpcEntity.Role.MINES, "Maître des mines", 1, "");
                    c.getSource().sendSuccess(() -> ok("Maître des mines posé : il ouvre le menu des cinq étages (écran de chargement puis arrivée dans la mine)."), true);
                    return 1;
                }));
    }

    private static int setDonjon(CommandContext<CommandSourceStack> c, DungeonBuilder.DungeonType only) {
        ServerLevel level = c.getSource().getLevel();
        if (c.getSource().getServer().getLevel(com.mmorpg.dungeon.InstanceManager.DIM) == null) {
            c.getSource().sendFailure(Component.literal("La dimension des donjons est introuvable (mmorpg:donjons) : le mod est-il à jour ?"));
            return 0;
        }
        String name = only == null ? "Maître des donjons" : "Gardien : " + only.label;
        String group = only == null ? com.mmorpg.dungeon.DungeonMaster.GROUP_ALL : "donjon:" + only.id;
        var npc = DungeonBuilder.spawnNpc(level, BlockPos.containing(c.getSource().getPosition()), com.mmorpg.entity.NpcEntity.Role.DONJON, name, 3, c.getSource().getRotation().y + 180);
        if (npc == null) return 0;
        npc.setup(com.mmorpg.entity.NpcEntity.Role.DONJON, name, 3, group);
        c.getSource().sendSuccess(() -> ok(name + " posé. " + (only == null ? "Il propose les " + DungeonBuilder.DungeonType.values().length + " donjons" : "Il propose « " + only.label + " »")
                + " : chaque joueur ou groupe y entre dans son propre exemplaire, construit automatiquement à la première visite (écran de chargement)."), true);
        return 1;
    }

    private static int playCinematic(ServerPlayer target, CommandContext<CommandSourceStack> c) {
        if (!ConfigManager.general().hubCinematic) {
            c.getSource().sendFailure(Component.literal("La cinématique est désactivée (hubCinematic dans general.json)."));
            return 0;
        }
        if (!com.mmorpg.hub.CinematicManager.start(target, false)) {
            c.getSource().sendFailure(Component.literal("Le hub n'est pas construit (/mmorpg hub)."));
            return 0;
        }
        c.getSource().sendSuccess(() -> ok("Cinématique lancée pour " + target.getGameProfile().name()), true);
        return 1;
    }

    private static int endTrial(ServerPlayer target, CommandContext<CommandSourceStack> c) {
        if (!com.mmorpg.hub.ClassTrial.active(target)) {
            c.getSource().sendFailure(Component.literal(target.getGameProfile().name() + " n'est pas en essai."));
            return 0;
        }
        com.mmorpg.hub.ClassTrial.end(target, true, "Essai terminé : votre personnage a été restauré.");
        c.getSource().sendSuccess(() -> ok("Essai terminé, personnage restauré."), true);
        return 1;
    }

    private static int spawnNpc(CommandContext<CommandSourceStack> c, com.mmorpg.entity.NpcEntity.Role role) {
        String name = StringArgumentType.getString(c, "nom");
        float yaw = c.getSource().getRotation().y + 180;
        int skin = role == com.mmorpg.entity.NpcEntity.Role.QUETES ? 0 : 1;
        if (name.startsWith("@")) {
            int sp = name.indexOf(' ');
            try {
                skin = Integer.parseInt(name.substring(1, sp < 0 ? name.length() : sp));
                name = sp < 0 ? role.label : name.substring(sp + 1);
            } catch (NumberFormatException ignored) {
            }
        }
        var npc = DungeonBuilder.spawnNpc(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), role, name, skin, yaw);
        String finalName = name;
        if (npc != null) c.getSource().sendSuccess(() -> ok(role.label + " « " + finalName + " » créé"), true);
        return npc != null ? 1 : 0;
    }

    private static int spawnMob(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        String key = StringArgumentType.getString(c, "type").toLowerCase(Locale.ROOT);
        if (BossManager.typeFor(key) != null) {
            BossManager.spawnBoss(c.getSource().getLevel(), BossManager.typeFor(key), key, BlockPos.containing(c.getSource().getPosition()), c.getSource().getTextName());
            return 1;
        }
        if (MobSpawner.spawnForCommand(c.getSource().getLevel(), key, BlockPos.containing(c.getSource().getPosition()), level) == null) {
            c.getSource().sendFailure(Component.literal("Monstre inconnu : " + key));
            return 0;
        }
        return 1;
    }
}
