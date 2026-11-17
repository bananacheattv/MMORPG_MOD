package com.mmorpg.plains;

import com.mmorpg.MMORPG;
import com.mmorpg.farm.AeloriaCropBlock;
import com.mmorpg.farm.FarmCrops;
import com.mmorpg.mine.MineTools;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Recolte des cultures du mod dans les zones declarees avec {@code "recolte": true} (voir {@code HuntZones}, /zone chasse recolte) :
 * la culture mure donne ses produits d'un coup, puis repousse comme une veine. Les anciennes instances « Plaines d'Aeloria » n'existent plus.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PlainsManager {
    private static final int[] CROP_COOLDOWN = {40, 55, 75, 95, 120};

    private PlainsManager() {
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        MinecraftServer server = event.getServer();
        if (PlainsData.get(server).respawns().isEmpty()) return;
        for (ServerLevel level : server.getAllLevels()) respawns(server, level);
    }

    // ================================================================== cultures : recolte a la facon des veines

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BreakBlockEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof AeloriaCropBlock crop)) return;
        if (!(event.getPlayer() instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!com.mmorpg.zone.HuntZones.cropZone(level, event.getPos())) return;
        if (sp.isCreative() && sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;      // un batisseur peut casser le bloc
        event.setCanceled(true);
        harvest(level, sp, event.getPos(), crop, state);
    }

    /** Les champs ne se piétinent pas : la terre labourée reste intacte. */
    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level && com.mmorpg.zone.HuntZones.cropZone(level, event.getPos())) event.setCanceled(true);
    }

    /** Recolte une culture mure (et, avec une houe d'Argentite ou mieux, celles qui l'entourent) ; elle repoussera comme une veine. */
    public static boolean harvest(ServerLevel level, ServerPlayer p, BlockPos pos, AeloriaCropBlock block, BlockState st) {
        ItemStack tool = p.getMainHandItem();
        int hoe = MineTools.hoeTier(tool);
        if (!gather(level, p, pos, block, st, hoe)) return false;
        if (hoe >= 1) tool.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        if (hoe < 2) return true;
        int r = Math.min(4,(hoe >= 4 ? 2 : 1)+com.mmorpg.mine.ToolRunes.rank(p.getMainHandItem(),2)/2);
        int n = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos at = pos.offset(dx, 0, dz);
                BlockState s2 = level.getBlockState(at);
                if (s2.getBlock() instanceof AeloriaCropBlock cb && cb.isMaxAge(s2) && gather(level, p, at, cb, s2, hoe)) n++;
            }
        }
        if (n > 0) p.sendOverlayMessage(Component.literal("Récolte en zone : +" + n + " plante" + (n > 1 ? "s" : "")).withColor(block.crop().color()));
        return true;
    }

    private static boolean gather(ServerLevel level, ServerPlayer p, BlockPos pos, AeloriaCropBlock block, BlockState st, int hoe) {
        FarmCrops.Crop crop = block.crop();
        if (!block.isMaxAge(st)) {
            p.sendOverlayMessage(Component.literal(crop.name() + " : pas encore mûre.").withColor(0xFFC060));
            return false;
        }
        PlayerData d = RpgPlayers.get(p);
        if (d.level < crop.minLevel()) {
            p.sendOverlayMessage(Component.literal("Niveau " + crop.minLevel() + " requis pour récolter la " + crop.name() + ".").withColor(0xFF7060));
            return false;
        }
        var random = level.getRandom();
        int rank = FarmCrops.CROPS.indexOf(crop);
        int count = 1 + (random.nextInt(100) < 12 + rank * 2 + hoe * 4 + com.mmorpg.mine.ToolRunes.rank(p.getMainHandItem(),0)*10 ? 1 : 0);
        for (int i = 0; i < 2; i++) if (random.nextInt(100) < hoe * 10) count++;
        if (com.mmorpg.job.JobSystem.rollDouble(p, com.mmorpg.job.Job.FERMIER, random)) count *= 2;
        ItemStack drop = new ItemStack(ModItems.get(crop.product()), count);
        if (!p.getInventory().add(drop)) level.addFreshEntity(new ItemEntity(level, p.getX(), p.getY() + 0.5, p.getZ(), drop));
        com.mmorpg.server.AdventureSummary.item(p,ModItems.get(crop.product()),count-drop.getCount());
        RpgPlayers.giveGatherXp(p, 2 + rank * 3L);
        com.mmorpg.job.JobSystem.addXp(p, com.mmorpg.job.Job.FERMIER, com.mmorpg.job.Job.cropXp(rank));
        com.mmorpg.job.JobSystem.farmerMeal(p, random);
        p.sendOverlayMessage(Component.literal("+" + count + " " + crop.name()).withColor(crop.color()));
        level.setBlock(pos, block.getStateForAge(0), 3);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F + random.nextFloat() * 0.3F);
        long wait = (long) (CROP_COOLDOWN[Math.min(rank, CROP_COOLDOWN.length - 1)] * 20 * (0.8 + random.nextDouble() * 0.4));
        PlainsData.get(level.getServer()).add(new PlainsData.Respawn(pos.getX(), pos.getY(), pos.getZ(), level.getGameTime() + wait, level.dimension().identifier().toString()));
        return true;
    }

    private static void respawns(MinecraftServer server, ServerLevel level) {
        PlainsData data = PlainsData.get(server);
        if (data.respawns().isEmpty()) return;
        long now = level.getGameTime();
        List<PlainsData.Respawn> due = new ArrayList<>();
        String dimId = level.dimension().identifier().toString();
        for (PlainsData.Respawn r : data.respawns()) {
            if (!r.dim().equals(dimId)) continue;                          // seules les entrees dont le chunk est charge comptent : les autres n'en bloquent pas d'autres
            if (r.due() <= now && level.hasChunkAt(r.pos())) due.add(r);
            if (due.size() >= 120) break;
        }
        for (PlainsData.Respawn r : due) {
            BlockPos pos = r.pos();
            if (!level.hasChunkAt(pos)) continue;                          // elle repoussera au prochain passage
            data.remove(r);
            if (level.getBlockState(pos).getBlock() instanceof AeloriaCropBlock cb) {
                level.setBlock(pos, cb.getStateForAge(cb.getMaxAge()), 3);
                if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 24, false) != null) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.25, 0.25, 0.25, 0.0);
                }
            }
        }
    }

    /** Test : fait repousser tout de suite les cultures recoltees. */
    public static int respawnAllNow(MinecraftServer server) {
        PlainsData data = PlainsData.get(server);
        int n = data.respawns().size();
        List<PlainsData.Respawn> all = new ArrayList<>(data.respawns());
        data.clear();
        for (PlainsData.Respawn r : all) data.add(new PlainsData.Respawn(r.x(), r.y(), r.z(), 0, r.dim()));
        return n;
    }
}
