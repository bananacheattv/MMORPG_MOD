package com.mmorpg.hub;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jspecify.annotations.Nullable;

/**
 * Donnees persistantes du joueur liees au hub : cinematique deja vue, classes essayees, et, pendant un essai, la sauvegarde
 * complete du personnage (donnees RPG, sac, equipement, position de retour). Cette sauvegarde est ecrite avec le joueur : si
 * le serveur plante ou si le joueur se deconnecte pendant l'essai, tout est restaure a sa reconnexion.
 */
public class HubPlayerData implements ValueIOSerializable {
    public boolean cinematicSeen;
    public int tutorialMask;
    /** Copie du hub attribuee au joueur (0 = Hub 1). */
    public int hub;
    /** Bit (ordinal de la classe) a 1 : la classe a deja ete essayee. */
    public int triedMask;
    public boolean inTrial;
    public int trialClass;
    public int trialLevel;
    public long trialStartTick;
    /** Sauvegarde du personnage avant l'essai. */
    public @Nullable CompoundTag snapshot;
    public @Nullable ListTag inventory;
    public String returnDim = "minecraft:overworld";
    public double returnX, returnY, returnZ;
    public float returnYaw, returnPitch;

    public boolean tried(int classOrdinal) {
        return (triedMask & (1 << classOrdinal)) != 0;
    }

    @Override
    public void serialize(ValueOutput out) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("cinematic", cinematicSeen);
        t.putInt("hub", hub);
        t.putInt("tried", triedMask);
        t.putInt("tutorial", tutorialMask);
        t.putBoolean("inTrial", inTrial);
        t.putInt("trialClass", trialClass);
        t.putInt("trialLevel", trialLevel);
        t.putLong("trialStart", trialStartTick);
        if (snapshot != null) t.put("snapshot", snapshot);
        if (inventory != null) t.put("inventory", inventory);
        t.putString("returnDim", returnDim);
        t.putDouble("rx", returnX);
        t.putDouble("ry", returnY);
        t.putDouble("rz", returnZ);
        t.putFloat("ryaw", returnYaw);
        t.putFloat("rpitch", returnPitch);
        out.store("hub", CompoundTag.CODEC, t);
    }

    @Override
    public void deserialize(ValueInput in) {
        in.read("hub", CompoundTag.CODEC).ifPresent(t -> {
            cinematicSeen = t.getBooleanOr("cinematic", false);
            hub = t.getIntOr("hub", 0);
            triedMask = t.getIntOr("tried", 0);
            tutorialMask = t.getIntOr("tutorial", 0);
            inTrial = t.getBooleanOr("inTrial", false);
            trialClass = t.getIntOr("trialClass", 0);
            trialLevel = t.getIntOr("trialLevel", 1);
            trialStartTick = t.getLongOr("trialStart", 0);
            snapshot = t.getCompound("snapshot").orElse(null);
            inventory = t.getList("inventory").orElse(null);
            returnDim = t.getStringOr("returnDim", "minecraft:overworld");
            returnX = t.getDoubleOr("rx", 0);
            returnY = t.getDoubleOr("ry", 0);
            returnZ = t.getDoubleOr("rz", 0);
            returnYaw = t.getFloatOr("ryaw", 0);
            returnPitch = t.getFloatOr("rpitch", 0);
        });
    }
}
