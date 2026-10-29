package com.mmorpg.network;

import com.mmorpg.MMORPG;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Demande au client d'ouvrir un ecran (choix de classe, forge, teleporteur...). */
public record S2COpenScreen(int screen, CompoundTag extra) implements CustomPacketPayload {
    public static final int CLASS_SELECT = 0;
    public static final int FORGE = 1;
    public static final int TELEPORTER = 2;
    public static final int TELEPORTER_SETUP = 3;
    public static final int QUEST_GIVER = 4;
    public static final int SHOP = 5;
    public static final int QUEST_EDITOR = 6;
    public static final int LUCKY_BLOCK = 7;
    public static final int LUCKY_RESULT = 8;
    public static final int LOOT_EDITOR = 9;
    public static final int CRATE = 10;
    /** Dialogue d'un Maitre de classe / de l'epreuve. */
    public static final int CLASS_MASTER = 11;
    /** Lancement de la cinematique d'arrivee (definition JSON et origine du hub). */
    public static final int CINEMATIC = 12;
    /** Etat de l'essai de classe (pas d'ecran) : actif, classe, niveau. */
    public static final int TRIAL_STATE = 13;
    public static final int DUNGEON_MASTER = 14;
    /** Menu du Maitre des mines (etages, niveaux, minerais). */
    public static final int MINE_MASTER = 15;
    /** Presse de compression (minerais compresses). */
    public static final int COMPRESSOR = 16;
    /** Banque d'Aeloria (solde et coffre) et hotel des ventes. */
    public static final int BANK = 17;
    public static final int AUCTION = 18;
    /** Pancarte des quetes hebdomadaires. */
    public static final int WEEKLY = 32;
    public static final int SPAWN_EDITOR = 19;
    public static final int ADVENTURE_SUMMARY = 20;
    public static final int MERCHANT_EDITOR = 21;
    public static final int PREMIUM_STORE=28,ARENA_HUD=29,ARENA_RESULTS=30,PVP_STORE=31,TRADE=33;
    public static final int GUILD=22,ARENA=23,ARENA_READY=24,HUB_TUTORIAL=25,ARMOR_STAND_EDITOR=26,HUB_GUIDE=27;

    public static final Type<S2COpenScreen> TYPE = new Type<>(MMORPG.id("open_screen"));
    public static final StreamCodec<ByteBuf, S2COpenScreen> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, S2COpenScreen::screen,
            ByteBufCodecs.COMPOUND_TAG, S2COpenScreen::extra,
            S2COpenScreen::new);

    public static S2COpenScreen classSelect() {
        return new S2COpenScreen(CLASS_SELECT, new CompoundTag());
    }

    public static S2COpenScreen compressor(BlockPos pos) {
        CompoundTag t = new CompoundTag();
        t.putLong("pos", pos.asLong());
        return new S2COpenScreen(COMPRESSOR, t);
    }

    public static S2COpenScreen forge(BlockPos pos) {
        CompoundTag t = new CompoundTag();
        t.putLong("pos", pos.asLong());
        return new S2COpenScreen(FORGE, t);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
