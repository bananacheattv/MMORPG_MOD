package com.mmorpg.mine;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Minage des minerais du mod, dans n'importe quelle dimension : extraire un minerai donne directement le lingot ; le bloc devient une
 * « veine epuisee » puis repousse apres un temps de recharge (conserve dans le monde). Les anciennes instances « Mines d'Aeloria » n'existent
 * plus : on pose ses minerais ou l'on veut dans l'openworld (blocs minerai_* du mod, onglet Objets).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class MineManager {
    private MineManager() {
    }

    /** Ancien Maitre des mines (PNJ de spawn.json ou pose avec /setmine) : les instances n'existent plus. */
    public static void open(ServerPlayer p, com.mmorpg.entity.NpcEntity npc) {
        p.sendSystemMessage(Component.literal("Les mines se trouvent désormais dans le monde ouvert d'Aeloria.").withColor(0xFFC060));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        MinecraftServer server = event.getServer();
        if (MineData.get(server).respawns().isEmpty()) return;
        for (ServerLevel level : server.getAllLevels()) respawns(server, level);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BreakBlockEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof MineOreBlock ore)) return;
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (sp.isCreative() && sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;      // un batisseur peut casser le bloc
        event.setCanceled(true);
        mineAndVein(level, sp, event.getPos(), ore.ore());
    }

    /** Extrait le minerai casse, puis (pioches d'Ambrasite et au-dela, accroupi) toute sa veine. */
    public static boolean mineAndVein(ServerLevel level, ServerPlayer p, BlockPos pos, MineBlocks.Ore ore) {
        boolean ok = mine(level, p, pos, ore);
        if (ok) vein(level, p, pos, ore);
        return ok;
    }

    /** Minerais d'une meme veine extraits d'un coup (accroupi) selon le palier de la pioche : Ambrasite 4, Glacium 8, Neantite 14. */
    public static int veinLimit(int pickTier) {
        return pickTier >= 5 ? 14 : pickTier == 4 ? 8 : pickTier == 3 ? 4 : 0;
    }

    /** Pioches d'Ambrasite et au-dela : accroupi, extrait aussi les minerais de meme espece qui touchent celui qu'on vient de briser. */
    private static void vein(ServerLevel level, ServerPlayer p, BlockPos start, MineBlocks.Ore ore) {
        int limit = veinLimit(MineTools.pickTier(p.getMainHandItem()));
        if (limit <= 0 || !p.isShiftKeyDown()) return;
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        seen.add(start);
        queue.add(start);
        int mined = 0;
        while (!queue.isEmpty() && mined < limit) {
            BlockPos at = queue.poll();
            for (int dx = -1; dx <= 1 && mined < limit; dx++) {
                for (int dy = -1; dy <= 1 && mined < limit; dy++) {
                    for (int dz = -1; dz <= 1 && mined < limit; dz++) {
                        BlockPos n = at.offset(dx, dy, dz);
                        if (!seen.add(n)) continue;
                        if (!(level.getBlockState(n).getBlock() instanceof MineOreBlock other) || !other.ore().id().equals(ore.id())) continue;
                        if (!mine(level, p, n, ore)) return;                                   // pioche cassee ou refus : on s'arrete
                        mined++;
                        queue.add(n);
                    }
                }
            }
        }
        if (mined > 0) p.sendOverlayMessage(Component.literal("Veine : +" + mined + " minerai" + (mined > 1 ? "s" : "") + " extrait" + (mined > 1 ? "s" : "") + " d'un coup").withColor(ore.color()));
    }

    /** Extrait un minerai : le lingot va dans le sac, le bloc s'epuise puis repousse. @return vrai si le minerai a ete extrait */
    public static boolean mine(ServerLevel level, ServerPlayer p, BlockPos pos, MineBlocks.Ore ore) {
        ItemStack tool = p.getMainHandItem();
        if (!tool.is(ItemTags.PICKAXES)) {
            p.sendOverlayMessage(Component.literal("Il faut une pioche pour extraire la " + ore.name() + ".").withColor(0xFFC060));
            return false;
        }
        int pick = MineTools.pickTier(tool);
        if (pick < ore.tier()) {
            String need = MineTools.TIER_ID[Math.min(ore.tier(), MineTools.TIER_ID.length - 1)];
            p.sendOverlayMessage(Component.literal("Votre pioche est trop fragile pour la " + ore.name() + " : il faut une pioche de palier « " + need + " » ou mieux.").withColor(0xFF7060));
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 1.4F);
            return false;
        }
        PlayerData d = RpgPlayers.get(p);
        if (d.level < ore.minLevel()) {
            p.sendOverlayMessage(Component.literal("Niveau " + ore.minLevel() + " requis pour extraire la " + ore.name() + ".").withColor(0xFF7060));
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 1.6F);
            return false;
        }
        var random = level.getRandom();
        int count = 1 + (random.nextInt(100) < 12 + ore.tier() * 2 + pick * 4 + com.mmorpg.mine.ToolRunes.rank(tool,0)*10 ? 1 : 0);      // veine riche : un lingot de plus (la pioche aide)
        if (com.mmorpg.job.JobSystem.rollDouble(p, com.mmorpg.job.Job.MINEUR, random)) count *= 2;
        ItemStack ingot = new ItemStack(ModItems.get(ore.ingotId()), count);
        if (!p.getInventory().add(ingot)) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, p.getX(), p.getY() + 0.5, p.getZ(), ingot));
        tool.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        RpgPlayers.giveGatherXp(p, 3 + ore.minLevel() / 2L);
        com.mmorpg.job.JobSystem.addXp(p, com.mmorpg.job.Job.MINEUR, com.mmorpg.job.Job.oreXp(ore.tier()));
        com.mmorpg.server.AdventureSummary.item(p,ModItems.get(ore.ingotId()),count-ingot.getCount());
        p.sendOverlayMessage(Component.literal("+" + count + " Lingot de " + ore.name() + (count > 1 ? "  (veine riche !)" : "")).withColor(ore.color()));
        rareDrop(level, p, pos, ore, pick);
        BlockState old = level.getBlockState(pos);
        level.setBlock(pos, MineBlocks.EPUISE.get().defaultBlockState(), 3);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, old), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 18, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.25, 0.3, 0.25, 0.02);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.9F + random.nextFloat() * 0.3F);
        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7F, 1.1F);
        long wait = (long) (ore.cooldown() * 20 * (0.8 + random.nextDouble() * 0.4));
        MineData.get(level.getServer()).add(new MineData.Respawn(pos.getX(), pos.getY(), pos.getZ(), ore.tier(), level.getGameTime() + wait, level.dimension().identifier().toString()));
        return true;
    }

    /** Chances (en %) par minerai extrait, avant bonus de pioche : {Aelium, Orichalque, Astralium} selon le palier du minerai. */
    private static final double[][] RARE_CHANCE = {
            {3.0, 0, 0}, {4.5, 0.5, 0}, {6.0, 1.2, 0.05}, {7.0, 2.0, 0.1}, {8.0, 3.0, 0.35}};

    /** Lingots rares, epiques et legendaires : tirage independant du lingot de base ; la pioche augmente toutes les chances de 25 % par palier. */
    private static void rareDrop(ServerLevel level, ServerPlayer p, BlockPos pos, MineBlocks.Ore ore, int pick) {
        double[] chance = RARE_CHANCE[Math.min(ore.tier(), RARE_CHANCE.length - 1)];
        double mult = (1.0 + 0.25 * pick)*(1+ToolRunes.rank(p.getMainHandItem(),1)*.15);
        String[] ids = {"lingot_aelium", "lingot_orichalque", "lingot_astralium"};
        String[] names = {"Lingot d'Aelium", "Lingot d'Orichalque", "Lingot d'Astralium"};
        int[] colors = {0x6EF0E0, 0xFF8A5A, 0xFFE070};
        var random = level.getRandom();
        for (int i = 2; i >= 0; i--) {
            if (chance[i] <= 0 || random.nextDouble() * 100.0 >= chance[i] * mult) continue;
            ItemStack drop = new ItemStack(ModItems.get(ids[i]));
            if (!p.getInventory().add(drop)) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, p.getX(), p.getY() + 0.5, p.getZ(), drop));
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12 + i * 14, 0.4, 0.4, 0.4, 0.05 + 0.03 * i);
            level.playSound(null, pos, i == 0 ? SoundEvents.AMETHYST_CLUSTER_BREAK : SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F + 0.2F * i, 1.4F - 0.2F * i);
            p.sendSystemMessage(Component.literal("✦ Trouvaille " + (i == 0 ? "rare" : i == 1 ? "épique" : "légendaire") + " : " + names[i] + " !").withColor(colors[i]));
            if (i == 2) {
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ " + p.getName().getString() + " a extrait un Lingot d'Astralium dans les Mines d'Aeloria !").withColor(colors[2]), false);
            }
            return;                                                                          // un seul lingot rare par minerai (le plus precieux d'abord)
        }
    }

    private static void respawns(MinecraftServer server, ServerLevel level) {
        MineData data = MineData.get(server);
        if (data.respawns().isEmpty()) return;
        long now = level.getGameTime();
        List<MineData.Respawn> due = new ArrayList<>();
        String dimId = level.dimension().identifier().toString();
        for (MineData.Respawn r : data.respawns()) {
            if (!r.dim().equals(dimId)) continue;                            // seules les entrees dont le chunk est charge comptent : les autres n'en bloquent pas d'autres
            if (r.due() <= now && level.hasChunkAt(r.pos())) due.add(r);
            if (due.size() >= 120) break;
        }
        for (MineData.Respawn r : due) {
            BlockPos pos = r.pos();
            if (!level.hasChunkAt(pos)) continue;                          // il repoussera au prochain passage dans le secteur
            data.remove(r);
            if (!level.getBlockState(pos).is(MineBlocks.EPUISE.get())) continue;
            MineBlocks.Ore ore = MineBlocks.ORES.get(Math.min(MineBlocks.ORES.size() - 1, r.tier()));
            level.setBlock(pos, MineBlocks.block(ore).defaultBlockState(), 3);
            if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 24, false) != null) {
                level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
        }
    }

    /** Test : fait repousser tout de suite les minerais epuises. */
    public static int respawnAllNow(MinecraftServer server) {
        MineData data = MineData.get(server);
        int n = data.respawns().size();
        List<MineData.Respawn> all = new ArrayList<>(data.respawns());
        data.clear();
        for (MineData.Respawn r : all) data.add(new MineData.Respawn(r.x(), r.y(), r.z(), r.tier(), 0, r.dim()));
        return n;
    }
}
