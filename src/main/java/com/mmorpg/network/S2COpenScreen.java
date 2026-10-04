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

    public static final Type<S2COpenScreen> TYPE = new Type<>(MMORPG.id("open_screen"));
    public static final StreamCodec<ByteBuf, S2COpenScreen> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, S2COpenScreen::screen,
            ByteBufCodecs.COMPOUND_TAG, S2COpenScreen::extra,
            S2COpenScreen::new);

    public static S2COpenScreen classSelect() {
        return new S2COpenScreen(CLASS_SELECT, new CompoundTag());
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
