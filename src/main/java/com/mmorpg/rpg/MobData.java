package com.mmorpg.rpg;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Donnees RPG d'un monstre : niveau, points de vie "virtuels" (independants de la sante vanilla, plafonnee a 1024),
 * attaque et defense. Synchronisees avec les clients pour l'affichage (barres de vie, cadre de cible, boss).
 */
public class MobData implements ValueIOSerializable {
    public String key = "";
    public int level = 0;
    public float hp = 1;
    public float maxHp = 1;
    public float atk = 1;
    public float def = 0;
    public boolean boss = false;
    public int phase = 0;
    public double xpMultiplier = 1;

    public static final StreamCodec<ByteBuf, MobData> STREAM_CODEC = StreamCodec.of((buf, d) -> {
        ByteBufCodecs.STRING_UTF8.encode(buf, d.key);
        ByteBufCodecs.VAR_INT.encode(buf, d.level);
        buf.writeFloat(d.hp);
        buf.writeFloat(d.maxHp);
        buf.writeBoolean(d.boss);
        ByteBufCodecs.VAR_INT.encode(buf, d.phase);
    }, buf -> {
        MobData d = new MobData();
        d.key = ByteBufCodecs.STRING_UTF8.decode(buf);
        d.level = ByteBufCodecs.VAR_INT.decode(buf);
        d.hp = buf.readFloat();
        d.maxHp = buf.readFloat();
        d.boss = buf.readBoolean();
        d.phase = ByteBufCodecs.VAR_INT.decode(buf);
        return d;
    });

    public boolean initialized() {
        return level > 0;
    }

    public float hpFraction() {
        return maxHp <= 0 ? 0 : Math.max(0, Math.min(1, hp / maxHp));
    }

    @Override
    public void serialize(ValueOutput out) {
        out.putString("key", key);
        out.putInt("level", level);
        out.putFloat("hp", hp);
        out.putFloat("maxHp", maxHp);
        out.putFloat("atk", atk);
        out.putFloat("def", def);
        out.putBoolean("boss", boss);
        out.putInt("phase", phase);
        out.putDouble("xpMult", xpMultiplier);
    }

    @Override
    public void deserialize(ValueInput in) {
        key = in.getStringOr("key", "");
        level = in.getIntOr("level", 0);
        hp = in.getFloatOr("hp", 1);
        maxHp = in.getFloatOr("maxHp", 1);
        atk = in.getFloatOr("atk", 1);
        def = in.getFloatOr("def", 0);
        boss = in.getBooleanOr("boss", false);
        phase = in.getIntOr("phase", 0);
        xpMultiplier = in.getDoubleOr("xpMult", 1);
    }
}
