package com.mmorpg.client;

import com.mmorpg.block.chest.DungeonChestBlockEntity;
import com.mmorpg.block.crate.CrateBlock;
import com.mmorpg.block.crate.CrateBlockEntity;
import com.mmorpg.block.crate.CrateTier;
import com.mmorpg.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;

import java.util.ArrayList;
import java.util.List;

/** Vitrine des coffres (-Pshowcase=coffres) : caisses par rarete (fermees puis ouvertes) et coffre de donjon anime. Rendu cote client uniquement. */
final class DevChestShowcase {
    private static int tick, wait;
    private static boolean loadingSeen;
    private static final List<BlockPos> crates = new ArrayList<>();
    private static BlockPos dungeon;

    private DevChestShowcase() {
    }

    static void phase(Minecraft mc, String me) {
        if (com.mmorpg.client.LoadingClient.active()) {
            loadingSeen = true;
            wait = 0;
            return;
        }
        if (!loadingSeen && ++wait < 1800) return;
        int t = ++tick;
        var level = mc.level;
        if (level == null || mc.player == null) return;
        if (t == 5) {
            mc.getWindow().setWindowed(1280, 720);
            mc.player.setYRot(0);
            mc.player.setXRot(18);
        }
        var look = Direction.fromYRot(mc.player.getYRot());
        Direction right = look.getClockWise();
        BlockPos base = mc.player.blockPosition().relative(look, 6);
        if (t == 20) {
            for (int i = 0; i < 7; i++) {
                BlockPos pos = base.relative(right, (i - 3) * 2);
                level.setBlock(pos, ModBlocks.CRATES.get(CrateTier.values()[i]).get().defaultBlockState().setValue(CrateBlock.FACING, look.getOpposite()), 3);
                crates.add(pos);
            }
            dungeon = base.relative(look, -3);
            level.setBlock(dungeon, ModBlocks.DUNGEON_CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, look.getOpposite()), 3);
        }
        if (t == 80) Minecraft.getInstance().gui.hud.toggle();
        if (t == 90) DevShowcase.shot(mc, "caisses_fermees");
        if (t == 100) {
            long now = level.getGameTime();
            List<String> reel = new ArrayList<>();
            for (int i = 0; i < 40; i++) reel.add("mmorpg:piece_or");
            for (BlockPos pos : crates) {
                if (level.getBlockEntity(pos) instanceof CrateBlockEntity be) be.start(reel, 34, now, mc.player.getUUID(), "Test", "");
            }
            if (level.getBlockEntity(dungeon) instanceof DungeonChestBlockEntity be) be.triggerEvent(1, 1);
        }
        if (t == 230) DevShowcase.shot(mc, "caisses_ouverture_debut");
        if (t == 270) DevShowcase.shot(mc, "caisses_ouvertes_etoile");
        if (t == 330) DevShowcase.shot(mc, "caisses_ouvertes_tenue");
        if (t == 340 && level.getBlockEntity(dungeon) instanceof DungeonChestBlockEntity be) be.triggerEvent(1, 0);
        if (t == 350) DevShowcase.shot(mc, "coffre_donjon_fermeture");
        if (t == 450) DevShowcase.shot(mc, "coffre_donjon_ferme");
        if (t == 460) DevShowcase.stop(mc);
    }
}
