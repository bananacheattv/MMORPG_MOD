package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Deux pièces secrètes à énigme, en plus des runes de {@link DungeonSecrets} :
 * <ul>
 * <li>« Salle de la mémoire » (couloir d'entrée) : le cristal montre brièvement une suite de quatre couleurs, il faut la rejouer dans l'ordre ;</li>
 * <li>« Salle du Sphinx » (avant la salle V) : une devinette, quatre réponses (feu, eau, ombre, vent).</li>
 * </ul>
 * Résoudre l'énigme fait tomber le mur fissuré et révèle un coffre. L'énigme dépend de la graine de l'expédition.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DungeonPuzzles {
    private DungeonPuzzles() {
    }

    private static final Block[] COLORS = {Blocks.REDSTONE_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.GOLD_BLOCK};
    private static final String[] COLOR_NAMES = {"Rouge", "Cyan", "Vert", "Jaune"};
    private static final Block[] ANSWERS = {Blocks.MAGMA_BLOCK, Blocks.BLUE_ICE, Blocks.OBSIDIAN, Blocks.CALCITE};
    private static final String[] ANSWER_NAMES = {"Feu", "Eau", "Ombre", "Vent"};
    private static final String[] RIDDLES = {
            "Je dévore tout ce que je touche, et je meurs si l'on me noie.",
            "Je prends la forme de mon vase et je tombe sans me blesser.",
            "Plus on m'éclaire, moins on me voit.",
            "Je n'ai ni corps ni voix, mais je renverse les arbres."};
    private static final int[] RIDDLE_ANSWER = {0, 1, 2, 3};

    // positions relatives a l'origine de l'exemplaire (x, z) ; hauteur : y = 0 = sol de l'interieur
    static final int MEM_Z = Layout.C0.z0() + 12;       // controles du couloir d'entree
    static final int SPH_Z = Layout.C5.z0() + 11;       // controles du couloir avant la salle V
    private static final int CTL_X = 2;

    private static final Map<String, Integer> PROGRESS = new HashMap<>();
    private static final Map<String, Long> COOLDOWN = new HashMap<>();

    /** Ordre des couleurs de la suite (permutation tiree de la graine). */
    public static int[] sequence(long seed) {
        int[] a = {0, 1, 2, 3};
        java.util.Random r = new java.util.Random(seed * 31 + 7);
        for (int i = 3; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
        return a;
    }

    public static void debugClearCooldown() { COOLDOWN.clear(); }

    public static int riddle(long seed) {
        return (int) Math.floorMod(seed / 5, (long) RIDDLES.length);
    }

    /** Décalage des réponses autour de l'estrade (la réponse « Feu » n'est pas toujours au même endroit). */
    private static int shift(long seed) {
        return (int) Math.floorMod(seed / 11, 4L);
    }

    private static void chamber(ServerLevel level, BlockPos o, int z, Block wall) {
        for (int x = 6; x <= 17; x++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int y = -1; y <= 5; y++) {
                    boolean edge = x == 17 || Math.abs(dz) == 5 || y == -1 || y == 5 || x == 6;
                    level.setBlock(o.offset(x, y, z + dz), edge ? wall.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        level.setBlock(o.offset(9, 1, z), Blocks.SEA_LANTERN.defaultBlockState(), 2);
    }

    private static void seal(ServerLevel level, BlockPos o, int z) {
        for (int x = 4; x <= 6; x++) for (int dz = -1; dz <= 1; dz++) for (int y = 0; y <= 2; y++)
            level.setBlock(o.offset(x, y, z + dz), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 2);
    }

    private static void unseal(ServerLevel level, BlockPos o, int z) {
        for (int x = 4; x <= 6; x++) for (int dz = -1; dz <= 1; dz++) for (int y = 0; y <= 2; y++)
            level.setBlock(o.offset(x, y, z + dz), Blocks.AIR.defaultBlockState(), 3);
        level.playSound(null, o.offset(5, 1, z), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.6F, 0.6F);
        level.sendParticles(ParticleTypes.POOF, o.getX() + 5.5, o.getY() + 1.5, o.getZ() + z + 0.5, 30, 0.6, 0.8, 1, 0.03);
    }

    private static void ctl(ServerLevel level, BlockPos o, int z, int dz, Block b) {
        BlockPos at = o.offset(CTL_X, 0, z + dz);
        level.setBlock(at.below(), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
        level.setBlock(at, b.defaultBlockState(), 2);
    }

    /** Pose les deux pièces (appelée à chaque construction / remise à neuf). */
    public static void build(ServerLevel level, BlockPos o, long seed) {
        // salle de la memoire
        chamber(level, o, MEM_Z, Blocks.POLISHED_DEEPSLATE);
        seal(level, o, MEM_Z);
        ctl(level, o, MEM_Z, -3, Blocks.SEA_LANTERN);
        for (int i = 0; i < 4; i++) ctl(level, o, MEM_Z, -1 + i, COLORS[i]);
        DungeonBuilderChest.place(level, o.offset(12, 0, MEM_Z), InstanceManager.slotAt(level.getServer(), level, o.getX(), o.getZ() + 12));
        // salle du Sphinx
        chamber(level, o, SPH_Z, Blocks.POLISHED_BLACKSTONE_BRICKS);
        seal(level, o, SPH_Z);
        ctl(level, o, SPH_Z, -3, Blocks.CHISELED_SANDSTONE);
        int sh = shift(seed);
        for (int i = 0; i < 4; i++) ctl(level, o, SPH_Z, -1 + i, ANSWERS[(i + sh) % 4]);
        DungeonBuilderChest.place(level, o.offset(12, 0, SPH_Z), InstanceManager.slotAt(level.getServer(), level, o.getX(), o.getZ() + 12));
        PROGRESS.keySet().removeIf(k -> k.startsWith(o.getX() + "," + o.getZ() + ","));
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!(p.level() instanceof ServerLevel level) || !level.dimension().equals(InstanceManager.DIM)) return;
        var slot = InstanceManager.slotAt(level.getServer(), level, p.getX(), p.getZ());
        if (slot == null || slot.owner.isEmpty()) return;
        BlockPos o = new BlockPos(slot.x, slot.y, slot.z);
        int rx = e.getPos().getX() - slot.x, rz = e.getPos().getZ() - slot.z, ry = e.getPos().getY() - slot.y;
        if (rx != CTL_X || ry != 0) return;
        if (rz >= MEM_Z - 3 && rz <= MEM_Z + 2) {
            e.setCanceled(true);
            memory(level, p, slot, o, rz - MEM_Z);
        } else if (rz >= SPH_Z - 3 && rz <= SPH_Z + 2) {
            e.setCanceled(true);
            sphinx(level, p, slot, o, rz - SPH_Z);
        }
    }

    private static boolean solved(ServerLevel level, BlockPos o, int z) {
        return level.getBlockState(o.offset(5, 1, z)).isAir();
    }

    private static void memory(ServerLevel level, ServerPlayer p, InstanceData.Slot slot, BlockPos o, int dz) {
        if (solved(level, o, MEM_Z)) return;
        int[] seq = sequence(slot.variantSeed);
        String key = slot.x + "," + slot.z + ",mem";
        if (dz == -3) {
            PROGRESS.put(key, 0);
            StringBuilder sb = new StringBuilder("Mémorisez : ");
            for (int i = 0; i < 4; i++) sb.append(i > 0 ? " · " : "").append(COLOR_NAMES[seq[i]]);
            p.sendOverlayMessage(Component.literal(sb.toString()).withColor(0x9CD8FF));
            level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1F, 1F);
            return;
        }
        int color = dz + 1;
        int step = PROGRESS.getOrDefault(key, 0);
        if (seq[step] == color) {
            step++;
            level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1F, 0.8F + step * 0.2F);
            if (step >= 4) {
                PROGRESS.remove(key);
                unseal(level, o, MEM_Z);
                p.sendSystemMessage(Component.literal("Le mur fissuré s'effondre : la salle de la mémoire est ouverte !").withColor(0xC090FF));
            } else {
                PROGRESS.put(key, step);
                p.sendOverlayMessage(Component.literal("Bien : " + step + "/4").withColor(0x80E080));
            }
        } else {
            PROGRESS.put(key, 0);
            level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1F, 0.6F);
            p.sendOverlayMessage(Component.literal("Mauvaise couleur : la suite recommence. Touchez le cristal pour la revoir.").withColor(0xFF8060));
        }
    }

    private static void sphinx(ServerLevel level, ServerPlayer p, InstanceData.Slot slot, BlockPos o, int dz) {
        if (solved(level, o, SPH_Z)) return;
        int riddle = riddle(slot.variantSeed);
        if (dz == -3) {
            p.sendSystemMessage(Component.literal("Sphinx : « " + RIDDLES[riddle] + " »  — Feu, Eau, Ombre ou Vent ?").withColor(0xE8C070));
            return;
        }
        int picked = (dz + 1 + shift(slot.variantSeed)) % 4;
        String key = slot.x + "," + slot.z + ",sph";
        long now = level.getGameTime();
        if (COOLDOWN.getOrDefault(key, 0L) > now) {
            p.sendOverlayMessage(Component.literal("Le Sphinx vous dévisage… patientez un instant.").withColor(0xFF8060));
            return;
        }
        if (picked == RIDDLE_ANSWER[riddle]) {
            unseal(level, o, SPH_Z);
            p.sendSystemMessage(Component.literal("« " + ANSWER_NAMES[picked] + " » — le Sphinx s'incline, la paroi s'ouvre !").withColor(0xC090FF));
        } else {
            COOLDOWN.put(key, now + 60);
            level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1F, 0.5F);
            p.sendOverlayMessage(Component.literal("« " + ANSWER_NAMES[picked] + " » n'est pas la bonne réponse.").withColor(0xFF8060));
        }
    }

    /** Coffre de récompense d'une pièce secrète (rempli à chaque construction). */
    private static final class DungeonBuilderChest {
        static void place(ServerLevel level, BlockPos at, InstanceData.Slot slot) {
            var t = slot == null ? com.mmorpg.world.DungeonBuilder.DungeonType.CRYPTE_GOBELINE : InstanceManager.typeOf(slot.pool);
            var rnd = level.getRandom();
            var loot = DungeonLoot.room(t, rnd);
            loot.add(new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.PIECE_OR.get(), 15 + t.level / 2 + rnd.nextInt(15)));
            com.mmorpg.world.DungeonBuilder.chest(level, at, loot, rnd);
        }
    }
}
