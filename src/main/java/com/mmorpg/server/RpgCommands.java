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
        List<String> classes = Arrays.stream(PlayerClass.values()).map(PlayerClass::id).toList();
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
                    sb.append(p.getName().getString()).append(" — ").append(data.playerClass.title(data.level)).append(" niv. ").append(data.level)
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
                .then(Commands.literal("donjon").then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(dungeons, b))
                        .executes(c -> {
                            DungeonBuilder.DungeonType t = DungeonBuilder.DungeonType.byId(StringArgumentType.getString(c, "type"));
                            if (t == null) {
                                c.getSource().sendFailure(Component.literal("Donjon inconnu"));
                                return 0;
                            }
                            BlockPos tp = DungeonBuilder.buildDungeon(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), t);
                            c.getSource().sendSuccess(() -> ok("Donjon « " + t.label + " » (niv. " + t.level + ") construit vers le sud. Téléporteur en " + tp.toShortString()), true);
                            return 1;
                        })))
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
