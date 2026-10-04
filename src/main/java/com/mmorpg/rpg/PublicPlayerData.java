package com.mmorpg.rpg;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** Informations publiques d'un joueur, visibles des joueurs proches (classe, niveau, cosmetiques equipes). */
public class PublicPlayerData {
    public int playerClass = 0;
    public int level = 1;
    public List<String> cosmetics = new ArrayList<>();
    public float hpFraction = 1;

    public static final StreamCodec<ByteBuf, PublicPlayerData> STREAM_CODEC = StreamCodec.of((buf, d) -> {
        ByteBufCodecs.VAR_INT.encode(buf, d.playerClass);
        ByteBufCodecs.VAR_INT.encode(buf, d.level);
        ByteBufCodecs.VAR_INT.encode(buf, d.cosmetics.size());
        for (String c : d.cosmetics) ByteBufCodecs.STRING_UTF8.encode(buf, c);
        buf.writeFloat(d.hpFraction);
    }, buf -> {
        PublicPlayerData d = new PublicPlayerData();
        d.playerClass = ByteBufCodecs.VAR_INT.decode(buf);
        d.level = ByteBufCodecs.VAR_INT.decode(buf);
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        for (int i = 0; i < n; i++) d.cosmetics.add(ByteBufCodecs.STRING_UTF8.decode(buf));
        d.hpFraction = buf.readFloat();
        return d;
    });

    public boolean sameAs(PublicPlayerData o) {
        return o != null && o.playerClass == playerClass && o.level == level && o.cosmetics.equals(cosmetics)
                && Math.abs(o.hpFraction - hpFraction) < 0.02f;
    }
}
