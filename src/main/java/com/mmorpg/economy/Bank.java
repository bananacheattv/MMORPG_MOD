package com.mmorpg.economy;

import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.phys.Vec3;

/**
 * Banque d'Aeloria : depot et retrait d'or (solde protege de la mort) et coffre de trois pages (deblocage par niveau).
 * Toutes les actions sont verifiees cote serveur et exigent d'etre pres du bloc de la banque.
 */
public final class Bank {
    private Bank() {
    }

    public static final int[] PAGE_LEVEL = {1, 25, 50};
    public static final int ACT_DEPOSIT = 0, ACT_WITHDRAW = 1, ACT_OPEN_PAGE = 2;

    private static boolean near(ServerPlayer p, BlockPos pos) {
        return p.level().getBlockState(pos).is(com.mmorpg.registry.ModBlocks.BANQUE.get()) && p.distanceToSqr(Vec3.atCenterOf(pos)) < 100;
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        RpgPlayers.depositCoins(p);
        BankData.Account a = BankData.get(p.level().getServer()).account(p.getUUID());
        CompoundTag t = new CompoundTag();
        t.putLong("pos", pos.asLong());
        t.putLong("bank", a.gold);
        t.putLong("wallet", RpgPlayers.get(p).gold);
        t.putInt("level", RpgPlayers.get(p).level);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.BANK, t));
    }

    /** @param amount montant (or) ; -1 pour tout. @param page page du coffre. */
    public static void action(ServerPlayer p, BlockPos pos, int action, long amount, int page) {if(!com.mmorpg.server.FeatureFlags.allow(p,"banque"))return;
        if (!near(p, pos) || com.mmorpg.hub.ClassTrial.refuseTransfer(p, null)) return;
        PlayerData d = RpgPlayers.get(p);
        BankData data = BankData.get(p.level().getServer());
        BankData.Account a = data.account(p.getUUID());
        ServerLevel level = p.level();
        switch (action) {
            case ACT_DEPOSIT -> {
                RpgPlayers.depositCoins(p);
                long n = amount < 0 ? d.gold : Math.min(amount, d.gold);
                if (n <= 0) {
                    p.sendOverlayMessage(Component.literal("Vous n'avez rien à déposer.").withColor(0xFF7060));
                } else {
                    RpgPlayers.takeGold(p, n);
                    a.gold += n;
                    data.setDirty();
                    com.mmorpg.db.Audit.log(p, com.mmorpg.db.Audit.BANK_DEPOSIT, n, "solde " + a.gold);
                    level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.COINS.get(), SoundSource.NEUTRAL, 0.8f, 0.9f);
                    p.sendOverlayMessage(Component.literal("Dépôt de " + n + " or. Solde en banque : " + a.gold).withColor(0xFFD040));
                }
                open(p, pos);
            }
            case ACT_WITHDRAW -> {
                long n = amount < 0 ? a.gold : Math.min(amount, a.gold);
                if (n <= 0) {
                    p.sendOverlayMessage(Component.literal("Votre compte est vide.").withColor(0xFF7060));
                } else {
                    a.gold -= n;
                    data.setDirty();
                    com.mmorpg.db.Audit.log(p, com.mmorpg.db.Audit.BANK_WITHDRAW, n, "solde " + a.gold);
                    RpgPlayers.transferGold(p, n, false);
                    level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.COINS.get(), SoundSource.NEUTRAL, 0.8f, 1.1f);
                    p.sendOverlayMessage(Component.literal("Retrait de " + n + " or. Solde en banque : " + a.gold).withColor(0xFFD040));
                }
                open(p, pos);
            }
            case ACT_OPEN_PAGE -> openPage(p, a, data, page);
            default -> {
            }
        }
    }

    private static void openPage(ServerPlayer p, BankData.Account a, BankData data, int page) {
        if (page < 0 || page >= BankData.PAGES) return;
        if (RpgPlayers.get(p).level < PAGE_LEVEL[page]) {
            p.sendOverlayMessage(Component.literal("Cette page du coffre s'ouvre au niveau " + PAGE_LEVEL[page] + ".").withColor(0xFF7060));
            return;
        }
        int base = page * BankData.PAGE_SLOTS;
        boolean[] ready = {false};
        SimpleContainer box = new SimpleContainer(BankData.PAGE_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (!ready[0]) return;
                for (int i = 0; i < BankData.PAGE_SLOTS; i++) a.items.set(base + i, getItem(i).copy());
                data.setDirty();
            }
        };
        for (int i = 0; i < BankData.PAGE_SLOTS; i++) box.setItem(i, a.items.get(base + i).copy());
        ready[0] = true;
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.7f, 1f);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.sixRows(id, inv, box),
                Component.literal("Banque d'Aeloria · Coffre " + (page + 1))));
    }
}
